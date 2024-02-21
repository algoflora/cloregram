(ns cloregram.test.infrastructure.handler
  (:require [cloregram.test.infrastructure.state :as state]
            [cloregram.test.infrastructure.users :refer [get-user-by-id inc-msg-id]]
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

(defn- get-user-info
  [msg]
   (let [user (get-user-by-id (:chat_id msg))
         uid (-> user :username keyword)]
     [user uid]))

(defmethod handler :sendMessage
  [msg]
  (log/debug "Incoming :sendMessage" msg)
  (let [[user uid] (get-user-info msg)
        mid (:msg-id (inc-msg-id uid))]
    (when (nil? (:main-msg-id user))
      (swap! state/users #(assoc-in % [uid :main-msg-id] mid)))
    (swap! state/users (fn [users] (update-in users [uid :messages] #(assoc % mid msg))))
    {:status 200
     :body {:ok true
            :result (assoc msg :message_id mid)}}))

(defn- update-text
  [uid msg]
  (swap! state/users
         (fn [users]
           (update-in users
                      [uid :messages]
                      #(-> %
                           (assoc-in [(:message_id msg) :text] (:text msg))
                           (assoc-in [(:message_id msg) :reply_markup] (:reply_markup msg))))))
  (get-in @state/users [uid :messages (:message_id msg)]))

(defmethod handler :editMessageText
  [msg]
  (log/debug "Incoming :editMessageText" msg)
  (let [[user uid] (get-user-info msg)]
    (when (not (contains? (:messages user) (:message_id msg)))
      (throw (ex-info "No message to edit for user!" {:user user
                                                     :message-id (:message_id msg)
                                                     :message msg})))
    (let [new-msg (update-text uid msg)]
      {:status 200
       :body {:ok true
              :result new-msg}})))
    ;; TODO: Handle situation when User deleted message manually

(defn- delete-msg
  [uid msg]
  (swap! state/users
         (fn [users]
           (update-in users [uid :messages] #(dissoc % (:message_id msg))))))

(defmethod handler :deleteMessage
  [msg]
  (log/debug "Incoming :deleteMessage" msg)
  (let [[user uid] (get-user-info msg)]
    (when (not (contains? (:messages user) (:message_id msg)))
      (throw (ex-info "No message to delete for user!" {:user user
                                                        :message-id (:message_id msg)
                                                        :message msg})))
    (delete-msg uid msg)
    {:status 200
     :body {:ok true}}))

(defmethod handler :sendDocument
  [msg]
  (log/debug "Incoming :sendDocument" msg)
  (let [msg (-> msg (update :reply_markup #(parse-string % true)) (update :chat_id #(Integer/parseInt %)))
        [user uid] (get-user-info msg)
        mid (:msg-id (inc-msg-id uid))]
    (swap! state/users (fn [users] (update-in users [uid :messages] #(assoc % mid msg))))
    {:status 200
     :body {:ok true
            :result (-> msg (assoc :message_id mid) (dissoc :document))}}))

(defmethod handler :sendInvoice
  [msg]
  (log/debug "Incoming :sendInvoice")
  (let [[user uid] (get-user-info msg)
        mid (:msg-id (inc-msg-id uid))]
    (swap! state/users (fn [users] (update-in users [uid :messages] #(assoc % mid msg))))
    (log/debug "USERS" @state/users)
    {:status 200
     :body {:ok true
            :result (assoc msg :message_id mid)}}))

(defmethod handler :answerPreCheckoutQuery
  [msg]
  (log/debug "Incoming :answerPreCheckoutQuery" msg)
  {:status 200
   :body true})
