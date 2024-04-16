(ns cloregram.impl.handler
  (:require [cloregram.impl.state :refer [system]]
            [cloregram.impl.api :refer [api-wrap]]
            [cloregram.impl.callbacks :as clb]
            [cloregram.users :as u]
            [cloregram.utils :as utl]
            [com.brunobonacci.mulog :as μ]
            [clojure.string :as str]
            [clojure.edn :as edn]))

(defn delete-message
  [{:keys [mid user]}]
  (api-wrap 'delete-message {:chat_id (:user/id user)
                             :message_id mid})
  (clb/delete user mid))

(defmulti main-handler #(some #{:message :callback_query :pre_checkout_query} (keys %)))

(defn- reset
  [user upd]
  (u/set-msg-id user 0)
  (μ/log ::msg-id-reset-warning :update upd :user user)
  (main-handler upd))

(defn- handle-dispatch
  [_ msg]
  (cond
    (contains? msg :successful_payment) :payment
    :else :default))

(defmulti ^:private handle handle-dispatch)

(defn- check-handler!
  [user]
  (let [user-handler (:user/handler-function user)
        common-handler (->> (utl/get-project-info) :name (format "%s.handler/common") symbol)]
    (when-not (= common-handler user-handler)
      (u/set-handler user common-handler nil))))

(defmethod handle :default
  [user msg]
  (let [handler (-> user :user/handler-function utl/resolver)
        args (-> user :user/handler-arguments (assoc :user user :message msg))]
    (check-handler! user)
    (μ/trace ::handler-default
             {:pairs [:handler-default/function (:handler-function user)
                      :handler-default/arguments args]
              :capture (fn [resp] {:handler-default/response resp})}
             (handler args))
    (delete-message {:user user
                     :mid (:message_id msg)})))

(defmethod handle :payment
  [user msg]
  (let [payment-handler-symbol-fallback 'cloregram.handler/payment
        payment-handler-symbol-project  (->> (utl/get-project-info)
                                             :name
                                             (format "%s.handler/payment")
                                             (symbol))
        payment-handler? (utl/resolver payment-handler-symbol-project)
        payment-handler (or payment-handler? (utl/resolver payment-handler-symbol-fallback))
        args {:user user
              :payment (:successful_payment msg)}]
    (μ/trace ::handler-payment
             {:pairs [:handler-payment/function (if payment-handler?
                                                  payment-handler-symbol-project
                                                  payment-handler-symbol-fallback)
                      :handler-payment/arguments args]
              :capture (fn [resp] {:handler-payment/response resp})}
             (payment-handler args))))

(defmethod main-handler :message
  [upd]
  (if (= "private" (get-in upd [:message :chat :type]))
    (let [msg (:message upd)
          user (u/load-or-create (:from msg))]
      (if (and (= "/start" (:text msg)) (some? (:user/msg-id user)) (not= 0 (:user/msg-id user)))
        (reset user upd)
        (handle user msg)))
    (μ/log ::non-private-chat-update-warning :non-private-chat-update-warning/update upd)))

(defmethod main-handler :callback_query
  [upd]
  (let [cbq (:callback_query upd)
        cb-uuid (-> cbq :data java.util.UUID/fromString)
        user (u/load-or-create (:from cbq))]
    (μ/trace ::handling-callback-query
             {:pairs [:handling-callback-query/callback-query cbq
                      :handling-callback-query/user user]
              :capture (fn [resp] {:handling-callback-query/response resp})}
             (clb/call user cb-uuid))))

(defmethod main-handler :pre_checkout_query
  [upd]
  (let [pcq (upd :pre_checkout_query)
        user (u/load-or-create (:from pcq))]
    (μ/log ::precheckout-query-answer-ok {:pre-checkout-query-answer-ok/precheckout-query pcq
                                          :pre-checkout-query-answer-ok/user user})
    (api-wrap 'answer-precheckout-query-ok (:id pcq))))

(defmethod main-handler nil
  [upd]
  (μ/log ::unhandable-update-warning :unhandable-updaye-warning/update upd))
