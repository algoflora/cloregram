(ns magic-tray-bot.test.infrastructure.client
  (:require [dialog.logger :as log]
            [tick.core :as t]
            [org.httpkit.client :refer [post]]
            [magic-tray-bot.utils :refer [keys-hyphens->underscores]]
            [magic-tray-bot.test.infrastructure.state :as state]
            [magic-tray-bot.test.infrastructure.users :as users]))

(defn- send-update
  [data]
  (let [upd-id (swap! state/update-id inc)
        upd (merge {:update_id upd-id} data)
        _ (log/debug "Sending update:" upd)
        resp @(post @state/webhook-address {:body upd})]
    (cond
      (some? (:error resp)) (throw (ex-info "Client error occured on sending update" {:response resp}))
      (not= 200 (:status resp)) (throw (ex-info "Error when sending update" {:response resp}))
      :else (log/debug "Update response:" resp))))

(defn- send-message
  [uid data]
  (let [user (users/inc-msg-id uid)]
    (send-update {:message (merge {:message_id (:msg-id user)
                                   :from (-> user
                                             (dissoc :msg-id :messages)
                                             (assoc :is-bot true)
                                             (keys-hyphens->underscores))
                                   :date (t/millis (t/between (t/epoch) (t/inst)))
                                   :chat {:id (:id user)
                                          :type "private"}} data)})))

(defn send-text
  ([uid text] (send-text uid text []))
  ([uid text entities]
   (send-message uid {:text text :entities entities})))
