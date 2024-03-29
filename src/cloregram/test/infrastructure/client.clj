(ns cloregram.test.infrastructure.client
  (:require [clojure.tools.logging :as log]
            [cheshire.core :refer [generate-string]]
            [org.httpkit.client :refer [post]]
            [cloregram.utils :refer [keys-hyphens->underscores]]
            [nano-id.core :refer [nano-id]]
            [cloregram.test.infrastructure.state :as state]
            [cloregram.test.infrastructure.users :as u]
            [resauce.core :as res]
            [fivetonine.collage.util :as clgu]
            [fivetonine.collage.core :as clg]
            [clojure.java.io :as io]))

(defn- send-update
  [uid data]
  (when (-> @state/users uid :waiting-for-response?)
    (throw (ex-info "Already waiting response! Unpredictable behaviour possible!"
                    {:user (uid @state/users)
                     :data data})))
  (let [upd-id (swap! state/update-id inc)
        upd (merge {:update_id upd-id} data)]
    (log/debug "Sending Update" {:address @state/webhook-address
                                 :update upd
                                 :virtual-user (uid @state/users)})
    (swap! state/users #(assoc-in % [uid :waiting-for-response?] true))
    (post @state/webhook-address {:body (generate-string upd)
                                  :headers {"X-Telegram-Bot-Api-Secret-Token" @state/webhook-token
                                            "Content-Type" "application/json"}}
          (fn async-callback [{:keys [status error] :as resp}]
            (cond
              (some? error) (throw (ex-info "<ASYNC> Client error occured on sending update" {:error error
                                                                                              :response resp
                                                                                              :update upd}))
              (not= 200 status) (throw (ex-info "<ASYNC> Error when sending update" {:status status
                                                                                     :response resp
                                                                                     :update upd}))
              :else (log/info "<ASYNC> Successful response on Update" {:response resp
                                                                       :update upd}))))))

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
   (log/info "Virtual user sending text Message..." {:virtual-user (uid @state/users)
                                                     :text text})
   (send-message uid {:text text :entities entities})))

(defn send-photo

  "Simulate sending photo with optional `caption` from resource `path` by user with username `uid`. Optionaly `entities` array can be provided for formatting caption."

  {:added "0.9.1"}

  ([uid path] (send-photo uid nil path))
  ([uid caption path] (send-photo uid caption [] path))
  ([uid caption entities path]
   (let [file-id (nano-id)
         res (io/resource path)]
     (log/info "Virtual user sending photo Message..." {:virtual-user (uid @state/users)
                                                        :photo-path (.getPath res)
                                                        :photo-caption caption})
     (swap! state/files #(assoc % file-id (io/file res)))
     (let [file (@state/files file-id)
           img (clgu/load-image file)]
       (send-message uid {:caption caption
                          :caption_entities entities
                          :photo [{:file_id file-id
                                   :file_unique_id file-id
                                   :width (.getWidth img)
                                   :height (.getHeight img)
                                   :file_size (.length file)}]})))))

(defn- assert-uid 
  [msg uid]
  (let [user (u/get-user-by-uid uid)]
    (when (not= (:chat_id msg) (:id user))
      (throw (ex-info "Wrong User interacting with Message!" {:expected-user user
                                                              :message msg})))))

(defn click-btn
  
  "Simulate clicking button in `row` and `col` or with text `btn` in message `msg` by user with username `uid`. Exception would be thrown if there is no expected button."

  {:added "0.9.0"}

  ([msg uid row col]
   (assert-uid msg uid)
   (log/info "Virtual user pressing button" {:virtual-user (uid @state/users)
                                             :button-row row
                                             :button-column col
                                             :message msg})
   (if-let [cbd (try (-> msg
                         :reply_markup
                         :inline_keyboard
                         (nth (dec row))
                         (nth (dec col))
                         :callback_data)
                     (catch IndexOutOfBoundsException e
                       (throw (ex-info "No expected button in Message!" {:button-row row
                                                                         :button-column col
                                                                         :message msg}))))]
     (send-callback-query uid cbd)
     (throw (ex-info "Nil value of :reply_markup, :inline_keyboard or :callback_data in Message!"
                     {:row row
                      :column col
                      :message msg})))
   msg)
  ([msg uid btn-text]
   (assert-uid msg uid)
   (log/info "Virtual user pressing button" {:virtual-user (uid @state/users)
                                             :button-text btn-text
                                             :message msg})
   (if-let [cbd (->> msg
                     :reply_markup
                     :inline_keyboard
                     (flatten)
                     (filter #(= btn-text (:text %)))
                     (first)
                     (:callback_data))]
     (send-callback-query uid cbd)
     (throw (ex-info "No expected button in Message!" {:button-text btn-text
                                                       :message msg})))
   msg))

(defn press-btn

  "DEPRECATED: use 'click-btn' instead.
  Simulate clicking button in `row` and `col` or with text `btn` in message `msg` by user with username `uid`. Exception would be thrown if there is no expected button."

  {:deprecated "0.9.0"}

  [& args]
  (apply click-btn args))

(defn pay-invoice

  "Simulate payment for invoice from message `msg` by user with username `uid`"

  {:added "0.5.2"}

  [msg uid]
  (assert-uid msg uid)
  (assert (contains? msg :invoice) (str "No invoice to pay in message " msg))
  (let [invoice (:invoice msg)
        user (uid @state/users)
        pcqid (nano-id)]
    (log/info "Virtual user paying invoice" {:virtual-user (uid @state/users)
                                             :invoice invoice})
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
