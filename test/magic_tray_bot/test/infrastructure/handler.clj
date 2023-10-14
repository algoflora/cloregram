(ns magic-tray-bot.test.infrastructure.handler
  (:require [magic-tray-bot.test.infrastructure.state :as state]
            [magic-tray-bot.test.infrastructure.users :refer [get-uid-by-id inc-msg-id]]
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
  (log/debug "MSG" msg)
  (let [uid (get-uid-by-id (:chat_id msg))
        mid (:msg-id (inc-msg-id uid))]
    (swap! state/users (fn [users] (update-in users [uid :messages] #(assoc % mid msg))))
    (log/debug "USERS:" @state/users)
    {:status 200
     :body {:ok true}}))
