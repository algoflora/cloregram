(ns cloregram.test.infrastructure.handler
  (:require [cloregram.test.infrastructure.state :as state]
            [cloregram.test.infrastructure.users :refer [get-user-by-id inc-msg-id]]
            [dialog.logger :as log]))

(defmulti handler #(keyword (:endpoint %)))

(defmethod handler :setWebhook
  [{:keys [url]}]
  (reset! state/webhook-address url)
  (log/info "Webhook address saved")
  (log/debug "Webhook address:" @state/webhook-address)
  {:status 200
   :body {:ok true}})

(defmethod handler :getWebhookInfo
  [_]
  (log/debug "Getting Webhook info...")
  {:status 200
   :body {:ok true
          :result {:url @state/webhook-address
                   :has_custom_certificate false
                   :pending_update_count 0}}}) ; TODO: Maybe need to check in future

(defmethod handler :sendMessage
  [msg]
  (log/debug "Got message" msg)
  (let [user (get-user-by-id (:chat_id msg))
        uid (-> user :username keyword)
        mid (:msg-id (inc-msg-id uid))]
    (when (nil? (:main-msg-id user))
      (swap! state/users #(assoc-in % [uid :main-msg-id] mid)))
    (swap! state/users (fn [users] (update-in users [uid :messages] #(assoc % mid msg))))
    {:status 200
     :body {:ok true
            :result (assoc msg :message_id mid)}}))
