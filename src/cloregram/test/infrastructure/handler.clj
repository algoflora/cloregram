(ns cloregram.test.infrastructure.handler
  (:require [cloregram.test.infrastructure.state :as state]
            [cloregram.test.infrastructure.client :as c]
            [cheshire.core :refer [parse-string]]
            [taoensso.timbre :as log]))


(defn- get-user-info
  ([msg] (get-user-info @state/users msg))
  ([users msg]
   (let [user (->> users
                   (filter (fn [[k v]] (= (:chat_id msg) (:id v))))
                   (first)
                   (val))
         uid (-> user :username keyword)]
     [user uid])))

(defmulti handler #(keyword (:endpoint %)))

(defmethod handler :setWebhook
  [{:keys [url secret_token]}]
  (log/info "Incoming :setWebhook" {:url url :secret_token secret_token})
  (reset! state/webhook-address url)
  (reset! state/webhook-token secret_token)
  (log/info "Webhook address and token saved" {:webhook-address url :webhook-token secret_token})
  {:status 200
   :body {:ok true}})

(defmethod handler :getWebhookInfo
  [_]
  (log/info "Incoming :getWebhookInfo")
  (let [wh-info {:url @state/webhook-address
                 :has_custom_certificate false
                 :pending_update_count 0}] ; TODO: Maybe need to check in future
    (log/info "Got webhook info" {:webhook-info wh-info})
    {:status 200
     :body {:ok true
            :result wh-info}}))

(defmethod handler :sendMessage
  [msg]
  (log/info "Incoming :sendMessage" {:message msg})
  (swap! state/users (fn [users]
                      (let [[user uid] (get-user-info users msg)
                            mid (inc (:msg-id user))]
                        (cond-> users
                          (nil? (:main-msg-id user)) (assoc-in [uid :main-msg-id] mid)
                          true (assoc-in [uid :msg-id] mid)
                          true (assoc-in [uid :messages mid] (assoc msg :message_id mid))
                          true (assoc-in [uid :waiting-for-response?] false)))))
  (let [[user _] (get-user-info msg)
        message (-> user :messages (get (:msg-id user)))]
    (log/info "Message accepted" {:message message
                                  :virtual-user user})
    {:status 200
     :body {:ok true
            :result message}}))

(defmethod handler :editMessageText
  [msg]
  (log/info "Incoming :editMessageText" {:message msg})
  (swap! state/users (fn [users]
                       (let [mid (:message_id msg)
                             [user uid] (get-user-info users msg)]
                         (when (not (contains? (:messages user) (:message_id msg)))
                           (throw (ex-info "No message to edit for user!"
                                           {:user user
                                            :message-id (:message_id msg)
                                            :message msg})))
                         (cond-> users
                           true (assoc-in [uid :messages mid :text] (:text msg))
                           
                           (contains? msg :reply_markup)
                           (assoc-in [uid :messages mid :reply_markup] (:reply_markup msg))
                           
                           true (assoc-in [uid :waiting-for-response?] false)))))
  (let [[user _] (get-user-info msg)
        message (-> user :messages (get (:message_id msg)))]
    (log/info "Message edited" {:message message
                                :virtual-user user})
    {:status 200
     :body {:ok true
            :result message}}))
;; TODO: Handle situation when User deleted message manually

(defmethod handler :deleteMessage
  [msg]
  (log/info "Incoming :deleteMessage" {:message msg})
  (swap! state/users (fn [users]
                      (let [mid (:message_id msg)
                            [user uid] (get-user-info users msg)]
                        (when (not (contains? (:messages user) mid))
                          (throw (ex-info "No message to delete for virtual user!"
                                          {:message-id mid
                                           :message msg
                                           :virtual-user user})))
                        (let [new-users (-> users
                                            (update-in [uid :messages] dissoc mid)
                                            (assoc-in [uid :waiting-for-response?] false))]
                          (log/info "Message deleted" {:message msg
                                                       :virtual-user (uid new-users)})
                          new-users))))
  {:status 200
   :body {:ok true}})

(defmethod handler :sendDocument
  [msg]
  (let [msg (-> msg
                (update :reply_markup #(parse-string % true))
                (update :chat_id #(Integer/parseInt %)))]
       (log/info "Incoming :sendDocument" {:message msg})
       (swap! state/users (fn [users]
                            (let [[user uid] (get-user-info users msg)
                                  mid (inc (:msg-id user))]
                                 (-> users
                                     (assoc-in [uid :msg-id] mid)
                                     (assoc-in [uid :messages mid] (assoc msg :message_id mid))
                                     (assoc-in [uid :waiting-for-response?] false)))))
       (let [[user _] (get-user-info msg)
             message (-> user :messages (get (:msg-id user)))]
            (log/info "Document accepted" {:message message
                                           :virtual-user user})
            {:status 200
             :body {:ok true
                    :result message}})))

(defmethod handler :sendInvoice
  [msg]
  (log/info "Incoming :sendInvoice" {:message msg})
  (swap! state/users (fn [users]
                      (let [[user uid] (get-user-info users msg)
                            mid (inc (:msg-id user))
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
                        (-> users
                            (assoc-in [uid :msg-id] mid)
                            (assoc-in [uid :messages mid] (assoc msg# :message_id mid))
                            (assoc-in [uid :waiting-for-response?] false)))))
  (let [[user _] (get-user-info msg)
        message (-> user :messages (get (:msg-id user)))]
    (log/info "Invoice accepted" {:message message
                                     :virtual-user user})
    {:status 200
     :body {:ok true
            :result message}}))

(defmethod handler :answerPreCheckoutQuery
  [msg]
  (log/debug "Incoming :answerPreCheckoutQuery" {:message msg})
  (let [pcq-data (@state/checkout-queries (:pre_checkout_query_id msg))]
    (when (not= true (:ok msg))
      (throw (ex-info "Precheckout query with error!" {:pre-checkout-query-data pcq-data 
                                                       :error (:error_message msg)})))
    (let [invoice (:invoice pcq-data)
          uid (:uid pcq-data)
          message {:successful_payment {:currency (:currency invoice)
                                        :total_amount (->> (:prices invoice)
                                                           (map :amount)
                                                           (apply +))
                                        :invoice_payload (:payload invoice)}}]
          (swap! state/users #(assoc-in % [uid :waiting-for-response?] false))
          (c/send-message uid message :silent)
          (log/info "Precheckout query processed. Successful payment sent." {:pre-checkout-query-data pcq-data
                                                                             :message message})
          {:status 200
           :body {:ok true}})))

(defmethod handler :getFile
  [msg]
  (log/debug "Incoming :getFile" {:message msg})
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
              :description "File not found!"}})))
