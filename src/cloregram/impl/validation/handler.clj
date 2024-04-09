(ns cloregram.impl.validation.handler
  (:require [cloregram.impl.validation.state :as state]
            [cloregram.validation.client :as c]
            [cheshire.core :refer [parse-string]]
            [com.brunobonacci.mulog :as μ]))

(defn- get-v-user-info
  ([msg] (get-v-user-info @state/v-users msg))
  ([v-users msg]
   (let [v-user (->> v-users
                     (filter (fn [[k v]] (= (:chat_id msg) (:id v))))
                     (first)
                     (val))
         vuid (-> v-user :username keyword)]
     [v-user vuid])))

(defmulti handler #(keyword (:endpoint %)))

(defmethod handler :setWebhook
  [{:keys [url secret_token]}]
  (reset! state/webhook-address url)
  (reset! state/webhook-token secret_token)
  {:status 200
   :body {:ok true}})

(defmethod handler :getWebhookInfo
  [_]
  (let [wh-info {:url @state/webhook-address
                 :has_custom_certificate false
                 :pending_update_count 0}] ; TODO: Maybe need to check in future
    {:status 200
     :body {:ok true
            :result wh-info}}))

(defmethod handler :sendMessage
  [msg]
  (μ/trace ::sendMessage {:pairs [:sendMessage/message msg]
                          :capture (fn [resp] {:sendMessage/response resp})}
           (swap! state/v-users (fn [v-users]
                               (let [[v-user vuid] (get-v-user-info v-users msg)
                                     mid (inc (:msg-id v-user))]
                                 (cond-> v-users
                                   (nil? (:main-msg-id v-user)) (assoc-in [vuid :main-msg-id] mid)
                                   true (assoc-in [vuid :msg-id] mid)
                                   true (assoc-in [vuid :messages mid] (assoc msg :message_id mid))
                                   true (assoc-in [vuid :waiting-for-response?] false)))))
           (let [[v-user _] (get-v-user-info msg)
                 message (-> v-user :messages (get (:msg-id v-user)))]
             {:status 200
              :body {:ok true
                     :result message}})))

(defmethod handler :editMessageText ; TODO: Handle situation when User deleted message manually
  [msg]
  (μ/trace ::editMessageText {:pairs [:editMessageText/message msg]
                              :capture (fn [resp] {:editMessageText/response resp})}
           (swap! state/v-users (fn [v-users]
                                (let [mid (:message_id msg)
                                      [v-user vuid] (get-v-user-info v-users msg)]
                                  (when (not (contains? (:messages v-user) (:message_id msg)))
                                    (throw (ex-info "No message to edit for user!"
                                                    {:virtual-user v-user
                                                     :message-id (:message_id msg)
                                                     :message msg})))
                                  (cond-> v-users
                                    true (assoc-in [vuid :messages mid :text] (:text msg))
                           
                                    (contains? msg :reply_markup)
                                    (assoc-in [vuid :messages mid :reply_markup] (:reply_markup msg))
                           
                                    true (assoc-in [vuid :waiting-for-response?] false)))))
           (let [[v-user _] (get-v-user-info msg)
                 message (-> v-user :messages (get (:message_id msg)))]
             {:status 200
              :body {:ok true
                     :result message}})))

(defmethod handler :deleteMessage
  [msg]
  (μ/trace ::deleteMessage {:pairs [:deleteMessage/message msg]
                            :capture (fn [resp] {:deleteMessage/response resp})}
           (swap! state/v-users (fn [v-users]
                                (let [mid (:message_id msg)
                                      [v-user vuid] (get-v-user-info v-users msg)]
                                  (when (not (contains? (:messages v-user) mid))
                                    (throw (ex-info "No message to delete for virtual user!"
                                                    {:message-id mid
                                                     :message msg
                                                     :virtual-user v-user})))
                                  (-> v-users
                                      (update-in [vuid :messages] dissoc mid)
                                      (assoc-in [vuid :waiting-for-response?] false)))))
           {:status 200
            :body {:ok true
                   :result true}}))

(defmethod handler :sendPhoto
  [msg]
  (μ/trace ::sendPhoto {:pairs [:sendPhoto/message msg]
                        :capture (fn [resp] {:sendPhoto/response resp})}
           (swap! state/v-users (fn [v-users]
                                (let [[v-user vuid] (get-v-user-info v-users msg)
                                      mid (inc (:msg-id v-user))]
                                  (-> v-users
                                      (assoc-in [vuid :msg-id] mid)
                                      (assoc-in [vuid :messages mid] (assoc msg :message_id mid))
                                      (assoc-in [vuid :waiting-for-response?] false)))))
           (let [[v-user _] (get-v-user-info msg)
                 message (-> v-user :messages (get (:msg-id v-user)))]
             {:status 200
              :body {:ok true
                     :result (let [file-id (subs (:photo message) 9)]
                               (update message (keyword file-id) prn-str))}})))

(defmethod handler :sendDocument
  [msg]
  (μ/trace ::sendDocument {:pairs [:sendDocument/message msg]
                           :capture (fn [resp] {:sendDocument/response resp})}
           (swap! state/v-users (fn [v-users]
                                  (let [[v-user vuid] (get-v-user-info v-users msg)
                                        mid (inc (:msg-id v-user))]
                                    (-> v-users
                                        (assoc-in [vuid :msg-id] mid)
                                        (assoc-in [vuid :messages mid] (assoc msg :message_id mid))
                                        (assoc-in [vuid :waiting-for-response?] false)))))
           (let [[v-user _] (get-v-user-info msg)
                 message (-> v-user :messages (get (:msg-id v-user)))]
             {:status 200
              :body {:ok true
                     :result (let [file-id (subs (:document message) 9)]
                               (update message (keyword file-id) prn-str))}})))

(defmethod handler :sendInvoice
  [msg]
  (μ/trace ::sendInvoice {:pairs [:sendInvoice/message msg]
                          :capture (fn [resp] {:sendInvoice/response resp})}
           (swap! state/v-users (fn [v-users]
                                  (let [[v-user vuid] (get-v-user-info v-users msg)
                                     mid (inc (:msg-id v-user))
                                     invoice (select-keys msg [:title
                                                               :description
                                                               :payload
                                                               :provider_token
                                                               :currency
                                                               :prices])
                                     msg# (-> msg
                                              (dissoc :title
                                                      :description
                                                      :payload
                                                      :provider_token
                                                      :currency
                                                      :prices)
                                              (assoc :invoice invoice))]
                                 (-> v-users
                                     (assoc-in [vuid :msg-id] mid)
                                     (assoc-in [vuid :messages mid] (assoc msg# :message_id mid))
                                     (assoc-in [vuid :waiting-for-response?] false)))))
           (let [[v-user _] (get-v-user-info msg)
                 message (-> v-user :messages (get (:msg-id v-user)))]
             {:status 200
              :body {:ok true
                     :result message}})))

(defmethod handler :answerPreCheckoutQuery
  [msg]
  (μ/trace ::answerPreCheckoutQuery {:pairs [:answerPreCheckoutQuery/message msg]
                                     :capture (fn [resp] {:answerPreCheckoutQuery/response resp})}
           (let [pcq-data (@state/checkout-queries (:pre_checkout_query_id msg))]
             (when (not= true (:ok msg))
               (throw (ex-info "Precheckout query with error!" {:pre-checkout-query-data pcq-data 
                                                                :error (:error_message msg)})))
             (let [invoice (:invoice pcq-data)
                   vuid (:vuid pcq-data)
                   message {:successful_payment {:currency (:currency invoice)
                                                 :total_amount (->> (:prices invoice)
                                                                    (map :amount)
                                                                    (apply +))
                                                 :invoice_payload (:payload invoice)}}]
               (swap! state/v-users #(assoc-in % [vuid :waiting-for-response?] false))
               (μ/trace ::send-successfull-payment
                        {:pairs [:send-successfull-payment/message message
                                 :send-successfull-payment/vuid vuid]
                         :capture (fn [resp] {:send-successfull-payment/response resp})}
                        (c/send-message vuid message :silent))
               {:status 200
                :body {:ok true}}))))

(defmethod handler :getFile
  [msg]
  (μ/trace ::getFile {:pairs [:getFile/message msg]
                      :capture (fn [resp] {:getFile/response resp})}
           (let [file-id (:file_id msg)
                 file (@state/files file-id)]
             (if file
               {:status 200
                :body {:ok true
                       :result {:file_id file-id
                                :file_unique_id file-id
                                :file_size (.length file)
                                :file_path (.encodeToString (java.util.Base64/getEncoder) (.getBytes file-id))}}}
               {:status 200
                :body {:ok false
                       :description "File not found!"}}))))
