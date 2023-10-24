(ns cloregram.test.infrastructure.client
  (:require [dialog.logger :as log]
            [clojure.test :refer [is]]
            [cheshire.core :refer [generate-string]]
            [tick.core :as t]
            [org.httpkit.client :refer [post]]
            [cloregram.system.state :refer [system]]
            [cloregram.utils :refer [keys-hyphens->underscores]]
            [cloregram.test.infrastructure.state :as state]
            [cloregram.test.infrastructure.users :as users]))

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

(defn- send-callback-query
  [uid cbq]
  (let [user (uid @state/users)]
    (send-update {:callback_query {:id (java.util.UUID/randomUUID)
                                   :from (-> user
                                             (dissoc :msg-id :messages)
                                             (assoc :is-bot true)
                                             (keys-hyphens->underscores))
                                   :data cbq}})))

(defn send-text
  ([uid text] (send-text uid text []))
  ([uid text entities]
   (send-message uid {:text text :entities entities})))

(defn check-text
  [msg text]
  (is (= text (:text msg)))
  msg)

(defn check-btns
  [msg & btns]
  (let [bs (->> (:reply_markup msg)
                (flatten)
                (map :text)
                (sort))]
    (is (= (sort btns)))
    msg))

(defn press-btn
  [msg uid btn]
  (let [cbq (->> (:reply_markup msg)
                 (flatten)
                 (filter #(= btn (:text %)))
                 (first)
                 (:callback_query))]
    (send-callback-query uid cbq)))
