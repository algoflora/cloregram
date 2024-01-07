(ns cloregram.handler
  (:require [cloregram.system.state :refer [bot system]]
            [cloregram.api :as api]
            [cloregram.users :as u]
            [cloregram.utils :as utl]
            [cloregram.callbacks :as clb]
            [dialog.logger :as log]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [telegrambot-lib.core :as tbot]))

(defn delete-message
  [{:keys [mid user]}]
  (utl/api-wrap tbot/delete-message (bot) {:chat_id (:user/id user)
                                           :message_id mid}))

(defmulti main-handler #(some #{:message :callback_query} (keys %)))

(defn reset
  [user upd]
  (u/set-msg-id user 0)
  (log/debug "Reset of msg-id of User" user)
  (main-handler upd))

(defmethod main-handler :message
  [upd]
  (if (= "private" (get-in upd [:message :chat :type]))
    (let [msg (:message upd)
          user (u/get-or-create (:from msg))]
      (if (and (= "/start" (:text msg)) (some? (:user/msg-id user)) (not= 0 (:user/msg-id user)))
        (reset user upd)
        (let [hdata (:user/handler user)
              handler (-> hdata first utl/resolver)
              common-handler (symbol (str (:name (utl/get-project-info)) ".handler/common"))
              args (-> hdata second edn/read-string (assoc :user user :message msg))]
          (when (not= handler common-handler)
            (u/set-handler user common-handler nil))
          (log/infof "Handling message %s from User %s" (utl/msg->str msg) (utl/username user)) ; TODO: info?
          (log/debugf "Calling %s with args %s" handler args)
          (handler args)
          (delete-message {:user user
                           :mid (:message_id msg)}))))
    (log/warn "Message from non-private chat!" upd)))

(defmethod main-handler :callback_query
  [upd]
  (let [cbq (:callback_query upd)
        user (u/get-or-create (:from cbq))]
    (log/infof "Handling callbak query for User %s" (utl/username user))
    (clb/call user (-> cbq :data java.util.UUID/fromString))))

(defn common
  [{:keys [user]}]
  (api/send-message user "Hello from Cloregram Framework!" []))
