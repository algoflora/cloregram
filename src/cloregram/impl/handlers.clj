(ns ^:no-doc cloregram.impl.handlers
  (:require [cloregram.impl.state :refer [system]]
            [cloregram.impl.api :refer [api-wrap delete-message]]
            [cloregram.impl.callbacks :as clb]
            [cloregram.impl.users :as u]
            [cloregram.dynamic :refer :all]
            [cloregram.utils :as utl]
            [com.brunobonacci.mulog :as μ]
            [clojure.string :as str]
            [clojure.edn :as edn]))

(defmulti main-handler #(some #{:message :callback_query :pre_checkout_query} (keys %)))

(defn- reset
  [user upd]
  (u/set-msg-id user 0)
  (μ/log ::msg-id-reset-warning :update upd :user user)
  (main-handler upd))

(defn- handle-dispatch
  [msg]
  (cond
    (contains? msg :successful_payment) :payment
    :else :default))

(defmulti ^:private handle handle-dispatch)

(defn- check-handler!
  [user]
  (let [user-handler (:user/handler-function user)
        main-handler (->> (utl/get-project-info) :name (format "%s.handlers/main") symbol)]
    (when-not (= main-handler user-handler)
      (u/set-handler user main-handler nil))))

(defmethod handle :default
  [msg]
  (let [handler (-> *current-user* :user/handler-function utl/resolver)
        args (-> *current-user* :user/handler-arguments (assoc :message msg))]
    (check-handler! *current-user*)
    (μ/trace ::handler-default
             {:pairs [:handler-default/function (:handler-function *current-user*)
                      :handler-default/arguments args]
              :capture (fn [resp] {:handler-default/response resp})}
             (handler args))
    (delete-message *current-user* (:message_id msg))))

(defmethod handle :payment
  [msg]
  (let [payment-handler-symbol-fallback 'cloregram.handlers/payment
        payment-handler-symbol-project  (->> (utl/get-project-info)
                                             :name
                                             (format "%s.handlers/payment")
                                             (symbol))
        payment-handler? (utl/resolver payment-handler-symbol-project)
        payment-handler (or payment-handler? (utl/resolver payment-handler-symbol-fallback))
        args {:payment (:successful_payment msg)}]
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
        (binding [*current-user* user]
          (μ/with-context {:*current-user* *current-user*}
            (handle msg)))))
    (μ/log ::non-private-chat-update-warning :non-private-chat-update-warning/update upd)))

(defmethod main-handler :callback_query
  [upd]
  (let [cbq (:callback_query upd)
        cb-uuid (-> cbq :data java.util.UUID/fromString)
        user (u/load-or-create (:from cbq))]
    (check-handler! user)
    (μ/trace ::handling-callback-query
             {:pairs [:handling-callback-query/callback-query cbq]
              :capture (fn [resp] {:handling-callback-query/response resp})}
             (binding [*current-user* user
                       *from-message-id* (-> cbq :message :message_id)]
               (μ/with-context {:*current-user* *current-user*
                                :*from-message-id* *from-message-id*}
                 (clb/call cb-uuid))))))

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
