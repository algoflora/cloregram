(ns cloregram.test.infrastructure.client
  (:require [dialog.logger :as log]
            [cheshire.core :refer [generate-string]]
            [org.httpkit.client :refer [post]]
            [cloregram.utils :refer [keys-hyphens->underscores]]
            [nano-id.core :refer [nano-id]]
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

(defn send-message
  "Simulate sending raw message represented in `data` to user with username `uid`. In most cases you don't need this function. Use it only if you definitely know what you are doing. Optionaly use `:silent` option to not save message in test user's state"
  {:changed "0.5.2"}
  [uid data & opts]
  (let [user (u/inc-msg-id uid)
        msg {:message (merge {:message_id (:msg-id user)
                              :from (-> user
                                        (dissoc :msg-id :messages)
                                        (assoc :is-bot true)
                                        (keys-hyphens->underscores))
                              :chat {:id (:id user)
                                     :type "private"}} data)}]
    (when (nil? (some #{:silent} opts)) (swap! state/users (fn [users]
                        (update-in users [uid :messages] #(assoc % (get-in msg [:message :message_id]) {:text (get-in msg [:message :text])})))))
    (send-update msg)))

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
  "Simulate sending `text` by user with username `uid`. Optionaly `entities` array can be provided for formatting message."
  ([uid text] (send-text uid text []))
  ([uid text entities]
   (log/infof "User %s sent message \"%s\"" uid text)
   (send-message uid {:text text :entities entities})))

(defn- assert-uid 
  [msg uid]
  (let [user (u/get-user-by-uid uid)]
    (assert (= (:chat_id msg) (:id user)) (format "User %s tried to do dsomething in message %s" user msg))))

(defn press-btn
  "Simulate clicking button in `row` and `col` or with text `btn` in message `msg` by user with username `uid`"
  ([msg uid row col]
   (assert-uid msg uid)
   (log/infof "User %s pressed button %d/%d" uid row col)
   (let [cbq (-> (:reply_markup msg)
                 (nth (dec row))
                 (nth (dec col))
                 (:callback_query))]
     (send-callback-query uid cbq)
     msg))
  ([msg uid btn]
   (assert-uid msg uid)
   (log/infof "User %s pressed button \"%s\"" uid btn)
   (let [cbq (->> (:reply_markup msg)
                  (flatten)
                  (filter #(= btn (:text %)))
                  (first)
                  (:callback_query))]
     (send-callback-query uid cbq)
     msg)))

(defn pay-invoice
  "Simulate payment for invoice from message `msg` by user with username `uid`"
  {:added "0.5.2"}
  [msg uid]
  (assert-uid msg uid)
  (assert (contains? msg :invoice) (str "No invoice to pay in message " msg))
  (let [invoice (:invoice msg)
        user (uid @state/users)
        pcqid (nano-id)]
    (log/infof "User %s paying invoice %s" uid invoice)
    (swap! state/checkout-queries #(assoc % pcqid {:uid uid :invoice invoice}))
    (send-update {:pre_checkout_query {:id pcqid
                                       :from (-> user
                                                 (dissoc :msg-id :messages)
                                                 (assoc :is-bot true)
                                                 (keys-hyphens->underscores))
                                       :currency (:currency invoice)
                                       :total_amount (->> (:prices invoice)
                                                          (map :amount)
                                                          (apply +))
                                       :invoice_payload (:payload invoice)}})
    msg))

