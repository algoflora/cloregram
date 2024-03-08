(ns cloregram.test.infrastructure.client
  (:require [dialog.logger :as log]
            [cheshire.core :refer [generate-string]]
            [org.httpkit.client :refer [post]]
            [cloregram.utils :refer [keys-hyphens->underscores]]
            [nano-id.core :refer [nano-id]]
            [cloregram.test.infrastructure.state :as state]
            [cloregram.test.infrastructure.users :as u]))

(defn- send-update
  [uid data]
  (when (-> @state/users uid :waiting-for-response?)
    (throw (ex-info "Already waiting response! Unpredictable behaviour possible!"
                    {:user (uid @state/users)
                     :data data})))
  (let [upd-id (swap! state/update-id inc)
        upd (merge {:update_id upd-id} data)]
    (log/debug (format "Sending update to %s: %s" @state/webhook-address upd))
    (swap! state/users #(assoc-in % [uid :waiting-for-response?] true))
    (post @state/webhook-address {:body (generate-string upd)
                                  :headers {"X-Telegram-Bot-Api-Secret-Token" @state/webhook-token
                                            "Content-Type" "application/json"}}
          (fn async-callback [{:keys [status error] :as resp}]
            (cond
              (some? error) (throw (ex-info "<ASYNC> Client error occured on sending update" resp))
              (not= 200 status) (throw (ex-info "<ASYNC> Error when sending update" resp))
              :else (log/debug "<ASYNC> Update response:" resp))))))

(defn send-message
  "Simulate sending raw message represented in `data` to user with username `uid`. In most cases you don't need this function. Use it only if you definitely know what you are doing. Optionaly use `:silent` option to not save message in test user's state"
  {:changed "0.5.2"}
  [uid data & opts]
  (swap! state/users (fn [users]
                      (let [user (uid users)
                            mid (inc (:msg-id user))
                            msg (merge {:message_id mid
                                        :from (-> user
                                                  (dissoc :msg-id :messages)
                                                  (assoc :is-bot true)
                                                  (keys-hyphens->underscores))
                                        :chat {:id (:id user)
                                               :type "private"}} data)]
                        (-> users
                            (assoc-in [uid :msg-id] mid)
                            (assoc-in [uid :messages mid]
                                      (cond-> msg
                                        (some #{:silent} opts) (assoc :silent true)))))))
  (send-update uid {:message (let [user (uid @state/users)
                                   mid (:msg-id user)]
                               (-> user :messages (get mid)))}))

(defn- send-callback-query
  [uid cbd]
  (let [user (uid @state/users)]
    (send-update uid {:callback_query {:id (java.util.UUID/randomUUID)
                                       :from (-> user
                                                 (dissoc :msg-id :messages)
                                                 (assoc :is-bot true)
                                                 (keys-hyphens->underscores))
                                       :data cbd}})))

(defn send-text
  "Simulate sending `text` by user with username `uid`. Optionaly `entities` array can be provided for formatting message."
  ([uid text] (send-text uid text []))
  ([uid text entities]
   (log/infof "User %s sent message \"%s\"" uid text)
   (send-message uid {:text text :entities entities})))

(defn- assert-uid 
  [msg uid]
  (let [user (u/get-user-by-uid uid)]
    (when (not= (:chat_id msg) (:id user))
      (throw (ex-info "Wrong User interacting with Message!" {:expected-user user
                                                      :message msg})))))

(defn press-btn
  "Simulate clicking button in `row` and `col` or with text `btn` in message `msg` by user with username `uid`. Exception would be thrown if there is no expected button."
  {:changed "0.8.1"}
  ([msg uid row col]
   (assert-uid msg uid)
   (log/infof "User %s pressed button %d/%d" uid row col)
   (let [cbd (try (-> msg
                      :reply_markup
                      :inline_keyboard
                      (nth (dec row))
                      (nth (dec col))
                      (:callback_data))
                  (catch IndexOutOfBoundsException e
                    (throw (ex-info "No expected button in Message!" {:row row
                                                                      :column col
                                                                      :message msg}))))]
     (send-callback-query uid cbd)
     msg))
  ([msg uid btn-text]
   (assert-uid msg uid)
   (log/infof "User %s pressing button \"%s\"..." uid btn-text)
   (if-let [cbd (->> msg
                     :reply_markup
                     :inline_keyboard
                     (flatten)
                     (filter #(= btn-text (:text %)))
                     (first)
                     (:callback_data))]
     (do (send-callback-query uid cbd)
         msg)
     (throw (ex-info "No expected button in Message!" {:button-text btn-text
                                                       :message msg})))))

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
    (send-update uid {:pre_checkout_query {:id pcqid
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
