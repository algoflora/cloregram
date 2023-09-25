(ns magic-tray-bot.test.infrastructure.handler
  (:require [magic-tray-bot.test.infrastructure.state :as state]
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
