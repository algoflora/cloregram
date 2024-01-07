(ns cloregram.test.infrastructure.client
  (:require [dialog.logger :as log]
            [cheshire.core :refer [generate-string]]
            [org.httpkit.client :refer [post]]
            [cloregram.system.state :refer [system]]
            [cloregram.utils :refer [keys-hyphens->underscores]]
            [cloregram.test.infrastructure.state :as state]
            [cloregram.test.infrastructure.users :as u]))

(defn- send-update
  [data]
  (let [upd-id (swap! state/update-id inc)
        upd (merge {:update_id upd-id} data)]
    (log/debug (format "Sending update to %s: %s" @state/webhook-address upd))
    (post @state/webhook-address {:body (generate-string upd)
                                  :headers {"X-Telegram-Bot-Api-Secret-Token" @state/webhook-token
                                            "Content-Type" "application/json"}}
          (fn async-callback [{:keys [status error] :as resp}]
            (cond
              (some? error) (throw (ex-info "Client error occured on sending update" resp))
              (not= 200 status) (throw (ex-info "Error when sending update" resp))
              :else (log/debug "<ASYNC> Update response:" resp))))))

(defn- send-message
  [uid data]
  (let [user (u/inc-msg-id uid)]
    (send-update {:message (merge {:message_id (:msg-id user)
                                   :from (-> user
                                             (dissoc :msg-id :messages)
                                             (assoc :is-bot true)
                                             (keys-hyphens->underscores))
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
   (log/infof "User %s sent message \"%s\"" uid text)
   (send-message uid {:text text :entities entities})))

(defn- assert-uid 
  [msg uid]
  (let [user (u/get-user-by-uid uid)]
    (assert (= (:chat_id msg) (:id user)) (format "User %s tried to push button in message %s" user msg))))

(defn press-btn
  ([msg uid row col]
   (assert-uid msg uid)
   (log/infof "User %s pressed button %d/%d" uid row col)
   (let [cbq (-> (:reply_markup msg)
                 (nth (dec row))
                 (nth (dec col))
                 (:callback_query))]
     (send-callback-query uid cbq)))
  ([msg uid btn]
   (assert-uid msg uid)
   (log/infof "User %s pressed button \"%s\"" uid btn)
   (let [cbq (->> (:reply_markup msg)
                  (flatten)
                  (filter #(= btn (:text %)))
                  (first)
                  (:callback_query))]
     (send-callback-query uid cbq))))
