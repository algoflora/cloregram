(ns cloregram.test.infrastructure.handler
  (:require [cloregram.test.infrastructure.state :as state]
            [cloregram.test.infrastructure.client :as c]
            [cloregram.test.infrastructure.users :as u]
            [cheshire.core :refer [parse-string]]
            [dialog.logger :as log]))

(defmulti handler #(keyword (:endpoint %)))

(defmethod handler :setWebhook
  [{:keys [url secret_token]}]
  (log/debug "Incoming :setWebhook" url secret_token)
  (reset! state/webhook-address url)
  (reset! state/webhook-token secret_token)
  (log/info "Webhook address and token saved")
  {:status 200
   :body {:ok true}})

(defmethod handler :getWebhookInfo
  [_]
  (log/debug "Incoming :getWebhookInfo")
  {:status 200
   :body {:ok true
          :result {:url @state/webhook-address
                   :has_custom_certificate false
                   :pending_update_count 0}}}) ; TODO: Maybe need to check in future

(defmethod handler :sendMessage
  [msg]
  (log/debug "Incoming :sendMessage" msg)
  (swap! state/users (fn [users]
                      (let [[user uid] (u/get-user-info users msg)
                            mid (inc (:msg-id user))]
                        (cond-> users
                          (nil? (:main-msg-id user)) (assoc-in [uid :main-msg-id] mid)
                          true (assoc-in [uid :msg-id] mid)
                          true (assoc-in [uid :messages mid] msg)
                          true (assoc-in [uid :waiting-for-response?] false)))))
  {:status 200
   :body {:ok true
          :result (assoc msg :message_id (-> (u/get-user-info @state/users msg)
                                             (first)
                                             :msg-id))}})

(defmethod handler :editMessageText
  [msg]
  (log/debug "Incoming :editMessageText" msg)
  (swap! state/users (fn [users]
                      (let [mid (:message_id msg)
                            [user uid] (u/get-user-info users msg)]
                        (when (not (contains? (:messages user) (:message_id msg)))
                          (throw (ex-info "No message to edit for user!"
                                          {:user user
                                           :message-id (:message_id msg)
                                           :message msg})))
                        (-> users
                            (assoc-in [uid :messages mid :text] (:text msg))
                            (assoc-in [uid :messages mid :reply_markup] (:reply_markup msg))
                            (assoc-in [uid :waiting-for-response?] false)))))
  {:status 200
   :body {:ok true
          :result (-> (u/get-user-info @state/users msg)
                      (first)
                      :messages
                      (get (:message_id msg)))}})
    ;; TODO: Handle situation when User deleted message manually

(defn- delete-msg
  [uid msg]
  (swap! state/users
         (fn [users]
           (update-in users [uid :messages] #(dissoc % (:message_id msg))))))

(defmethod handler :deleteMessage
  [msg]
  (log/debug "Incoming :deleteMessage" msg)
  (swap! state/users (fn [users]
                      (let [mid (:message_id msg)
                            [user uid] (u/get-user-info users msg)]
                        (when (not (contains? (:messages user) (:message_id msg)))
                          (throw (ex-info "No message to delete for user!"
                                          {:user user
                                           :message-id (:message_id msg)
                                           :message msg})))
                        (-> users
                            (update-in [uid :messages] dissoc mid)
                            (assoc-in [uid :waiting-for-response?] false)))))
  {:status 200
   :body {:ok true}})

(defmethod handler :sendDocument
  [msg]
  (log/debug "Incoming :sendDocument" msg)
  (let [msg (-> msg
                (update :reply_markup #(parse-string % true))
                (update :chat_id #(Integer/parseInt %)))]
    (swap! state/users (fn [users]
                        (let [[user uid] (u/get-user-info users msg)
                              mid (inc (:msg-id user))]
                          (-> users
                              (assoc-in [uid :msg-id] mid)
                              (assoc-in [uid :messages mid] msg)
                              (assoc-in [uid :waiting-for-response?] false)))))
    {:status 200
     :body {:ok true
            :result (-> msg
                        (assoc :message_id (-> (u/get-user-info @state/users msg)
                                               (first)
                                               :msg-id))
                        (dissoc :document))}}))

(defmethod handler :sendInvoice
  [msg]
  (log/debug "Incoming :sendInvoice")
  (swap! state/users (fn [users]
                      (let [[user uid] (u/get-user-info users msg)
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
                            (assoc-in [uid :messages mid] msg#)
                            (assoc-in [uid :waiting-for-response?] false)))))
  {:status 200
   :body {:ok true
          :result (assoc msg :message_id (-> (u/get-user-info @state/users msg)
                                             (first)
                                             :msg-id))}})

(defmethod handler :answerPreCheckoutQuery
  [msg]
  (log/debug "Incoming :answerPreCheckoutQuery" msg)
  (when (not= true (:ok msg))
    (throw (ex-info "Precheckout query with error!" {:error (:error_message msg)})))
  (let [pcq-data (@state/checkout-queries (:pre_checkout_query_id msg))
        invoice (:invoice pcq-data)
        uid (:uid pcq-data)]
    (swap! state/users #(assoc-in % [uid :waiting-for-response?] false))
    (c/send-message uid {:successful_payment {:currency (:currency invoice)
                                              :total_amount (->> (:prices invoice)
                                                                 (map :amount)
                                                                 (apply +))
                                              :invoice_payload (:payload invoice)}} :silent)
    {:status 200
     :body true}))
