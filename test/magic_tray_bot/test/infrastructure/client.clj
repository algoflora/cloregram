(ns magic-tray-bot.test.infrastructure.client
  (:require [dialog.logger :as log]
            [clojure.test :refer [is]]
            [cheshire.core :refer [generate-string]]
            [tick.core :as t]
            [org.httpkit.client :refer [post]]
            [magic-tray-bot.system.state :refer [system]]
            [magic-tray-bot.utils :refer [keys-hyphens->underscores]]
            [magic-tray-bot.test.infrastructure.state :as state]
            [magic-tray-bot.test.infrastructure.users :as users]))

(defn- send-update
  [data]
  (let [upd-id (swap! state/update-id inc)
        upd (merge {:update_id upd-id} data)]
    (log/debug (format "Sending update to %s: %s" @state/webhook-address upd))
    (post @state/webhook-address {:body (generate-string upd)
                                  :headers {"X-Telegram-Bot-Api-Secret-Token" (:bot/webhook-key @system)
                                            "Content-Type" "application/json"}}
          (fn async-callback [{:keys [status error] :as resp}]
            (cond
              (some? error) (throw (ex-info "Client error occured on sending update" resp))
              (not= 200 status) (throw (ex-info "Error when sending update" resp))
              :else (log/debug "<ASYNC> Update response:" resp))))))

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

(defn check-text
  [text msg]
  (is (= text (:text msg)))
  msg)
