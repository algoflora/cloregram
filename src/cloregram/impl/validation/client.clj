(ns ^:no-doc cloregram.impl.validation.client
  (:require [cloregram.validation.users :as vu]
            [cloregram.impl.validation.state :as state]
            [com.brunobonacci.mulog :as μ]
            [fivetonine.collage.util :as clgu]
            [clojure.java.io :as io]
            [nano-id.core :refer [nano-id]]
            [cheshire.core :refer [generate-string]]
            [cheshire.generate :as generate]
            [org.httpkit.client :refer [post]]))

(generate/add-encoder java.io.File
  (fn [file json-generator]
    (generate/write-string json-generator (prn-str file))))

(defn- keys-hyphens->underscores ; NOT recursive!
  [m]
  (into {} (map (fn [[k v]] [(-> k name (.replace \- \_) keyword) v]) m)))

(defn send-update
  [vuid data]
  (when (-> @state/v-users vuid :waiting-for-response?)
    (throw (ex-info "Already waiting response! Unpredictable behaviour possible!"
                    {:virtual-user (vuid @state/v-users)
                     :data data})))
  (let [upd-id (swap! state/update-id inc)
        uuuid (nano-id)
        upd (merge {:update_id upd-id} data)
        ctx (μ/local-context)]
    (μ/log ::sending-update
           :sending-update/update-uuid uuuid
           :sending-update/address @state/webhook-address
           :sending-update/update upd
           :sending-update/virtual-user (vuid @state/v-users))
    (swap! state/v-users #(assoc-in % [vuid :waiting-for-response?] true))
    (post @state/webhook-address {:body (generate-string upd)
                                  :headers {"X-Telegram-Bot-Api-Secret-Token" @state/webhook-token
                                            "Content-Type" "application/json"
                                            "mulog-pass-root-trace" (:mulog/root-trace ctx)
                                            "mulog-pass-parent-trace" (:mulog/parent-trace ctx)}}
          (fn async-callback [{:keys [status error] :as resp}]
            (cond
              (some? error) (throw (ex-info "<ASYNC> Client error occured on sending update"
                                            {:error error
                                             :response resp
                                             :uuuid uuuid
                                             :update upd}))
              (not= 200 status) (throw (ex-info "<ASYNC> Error when sending update"
                                                {:status status
                                                 :response resp
                                                 :uuuid uuuid
                                                 :update upd}))
              :else (μ/log ::async-update-response
                           :async-update-response/response resp
                           :async-update-response/update-uuid uuuid
                           :async-update-response/update upd))))
    vuid))

(defn send-callback-query
  [msg vuid cbd]
  (let [v-user (vuid @state/v-users)]
    (send-update vuid {:callback_query {:id (java.util.UUID/randomUUID)
                                        :from (-> v-user
                                                  (dissoc :msg-id :messages)
                                                  (assoc :is-bot true)
                                                  (keys-hyphens->underscores))
                                        :message msg
                                        :data cbd}})))

(defn- assert-vuid 
  [msg vuid]
  (let [v-user (vu/get-v-user-by-vuid vuid)]
    (when (nil? msg)
      (throw (ex-info "Interacting with nil Message!" {:virtual-user v-user})))
    (when (not= (:chat_id msg) (:id v-user))
      (throw (ex-info "Wrong User interacting with Message!" {:virtual-user v-user
                                                              :message msg})))))

(defn send-message
  [vuid data & opts]
  (swap! state/v-users (fn [v-users]
                         (let [v-user (vuid v-users)
                               mid (inc (:msg-id v-user))
                               msg (merge {:message_id mid
                                           :from (-> v-user
                                                     (dissoc :msg-id :messages)
                                                     (assoc :is-bot true)
                                                     (keys-hyphens->underscores))
                                           :chat {:id (:id v-user)
                                                  :type "private"}} data)]
                           (-> v-users
                               (assoc-in [vuid :msg-id] mid)
                               (assoc-in [vuid :messages mid]
                                         (cond-> msg
                                           (some #{:silent} opts) (assoc :silent true)))))))
  (send-update vuid {:message (let [v-user (vuid @state/v-users)
                                    mid (:msg-id v-user)]
                                (-> v-user :messages (get mid)))}))

(defn send-photo
  [vuid caption entities path]
  (let [file-id (nano-id)
        res (io/resource path)]
    (swap! state/files #(assoc % file-id (io/file res)))
    (let [file (@state/files file-id)
          img (clgu/load-image file)]
      (send-message vuid {:caption caption
                          :caption_entities entities
                          :photo [{:file_id file-id
                                   :file_unique_id file-id
                                   :width (.getWidth img)
                                   :height (.getHeight img)
                                   :file_size (.length file)}]}))))

(defn click-btn
  ([msg vuid row col]
   (assert-vuid msg vuid)
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
     (send-callback-query msg vuid cbd)
     (throw (ex-info "Nil value of :reply_markup, :inline_keyboard or :callback_data in Message!"
                     {:row row
                      :column col
                      :message msg})))
   msg)
  ([msg vuid btn-text]
   (assert-vuid msg vuid)
   (if-let [cbd (->> msg
                     :reply_markup
                     :inline_keyboard
                     (flatten)
                     (filter #(= btn-text (:text %)))
                     (first)
                     (:callback_data))]
     (send-callback-query msg vuid cbd)
     (throw (ex-info "No expected button in Message!" {:button-text btn-text
                                                       :message msg})))
   msg))

(defn pay-invoice
  [msg vuid]
  (assert-vuid msg vuid)
  (assert (contains? msg :invoice) (str "No invoice to pay in message " msg))
  (let [invoice (:invoice msg)
        v-user (vuid @state/v-users)
        pcqid (nano-id)]
    (swap! state/checkout-queries #(assoc % pcqid {:vuid vuid :invoice invoice}))
    (send-update vuid {:pre_checkout_query {:id pcqid
                                            :from (-> v-user
                                                      (dissoc :msg-id :messages)
                                                      (assoc :is-bot true)
                                                      (keys-hyphens->underscores))
                                            :currency (:currency invoice)
                                            :total_amount (->> (:prices invoice)
                                                               (map :amount)
                                                               (apply +))
                                            :invoice_payload (:payload invoice)}})
    msg))
