(ns cloregram.handler
  (:require [cloregram.system.state :refer [bot system]]
            [cloregram.api :as api]
            [cloregram.users :as u]
            [cloregram.utils :as utl]
            [cloregram.callbacks :as clb]
            [taoensso.timbre :as log]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [telegrambot-lib.core :as tbot]))

(defn delete-message
  [{:keys [mid user]}]
  (utl/api-wrap tbot/delete-message (bot) {:chat_id (:user/id user)
                                           :message_id mid}))

(defmulti main-handler #(some #{:message :callback_query :pre_checkout_query} (keys %)))

(defn- reset
  [user upd]
  (u/set-msg-id user 0)
  (log/warn "Reset of msg-id called" {:update upd
                                      :user user})
  (main-handler upd))

(defn- handle-dispatch
  [user msg]
  (cond
    (contains? msg :successful_payment) :payment
    :else :text))

(defmulti ^:private handle handle-dispatch)

(defmethod handle :payment
  [user msg]
  (let [payment-handler (or (->> (utl/get-project-info)
                                 :name
                                 (format "%s.handler/payment")
                                 (symbol)
                                 (utl/resolver))
                            (utl/resolver 'cloregram.handler/payment))
        args {:user user
              :payment (:successful_payment msg)}]
    (log/debug "Handling payment Message..." {:payment-handler payment-handler
                                              :arguments args
                                              :message msg
                                              :user user})
    (payment-handler args)))

(defmethod handle :text
  [user msg]
  (let [hdata (:user/handler user)
        handler (-> hdata first utl/resolver)
        common-handler (symbol (str (:name (utl/get-project-info)) ".handler/common"))
        args (-> hdata second edn/read-string (assoc :user user :message msg))]
    (when (not= handler common-handler)
      (u/set-handler user common-handler nil))
    (log/debug "Handling text Message..." {:handler handler
                                           :arguments (or args {})
                                           :message msg
                                           :user user})
    (handler args)
    (delete-message {:user user
                     :mid (:message_id msg)})))

(defmethod main-handler :message
  [upd]
  (if (= "private" (get-in upd [:message :chat :type]))
    (let [msg (:message upd)
          user (u/get-or-create (:from msg))]
      (if (and (= "/start" (:text msg)) (some? (:user/msg-id user)) (not= 0 (:user/msg-id user)))
        (reset user upd)
        (handle user msg)))
    (log/warn "Update from non-private chat!" {:update upd})))

(defmethod main-handler :callback_query
  [upd]
  (let [cbq (:callback_query upd)
        user (u/get-or-create (:from cbq))]
    (log/debug "Handling Callback Query..." {:callback-query cbq
                                            :user user})
    (clb/call user (-> cbq :data java.util.UUID/fromString))))

(defmethod main-handler :pre_checkout_query
  [upd]
  (let [pcq (upd :pre_checkout_query)
        user (u/get-or-create (:from pcq))]
    (log/debug "Handling Precheckout Query..." {:pre-checkout-query pcq
                                               :user user})
    (utl/api-wrap tbot/answer-precheckout-query-ok (bot) (:id pcq))))

(defmethod main-handler nil
  [upd]
  (log/warn "main-handler dispatch function returned nil!" {:update upd}))

(defn common
  [{:keys [user]}]
  (api/send-message user "Hello from Cloregram Framework!" []))

(defn payment
  [{:keys [user payment]}]
  (api/send-message user (str "Successful payment with payload " (:invoice_payload payment)) [] :temp))
