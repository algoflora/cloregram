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

(defmulti main-handler #(some #{:message :callback_query} (keys %)))

(defmethod main-handler :message
  [upd]
  (let [msg (:message upd)
        user (u/get-or-create (:from msg))
        hdata (:user/handler user)
        handler (-> hdata first utl/resolver)
        args (-> hdata second edn/read-string (assoc :user user :message msg))]
    (log/infof "Handling message %s from User %s" (utl/msg->str msg) (utl/username user)) ; TODO: info?
    (log/debugf "Calling %s with args %s" handler args)
    (handler args)))

(defmethod main-handler :callback_query
  [upd]
  (let [cbq (:callback_query upd)
        user (u/get-or-create (:from cbq))]
    (log/infof "Handling callbak query for User %s" (utl/username user))
    (clb/call user (-> cbq :data java.util.UUID/fromString))))

(defn common
  [{:keys [user]}]
  (api/send-message user "Hello from Cloregram Framework!" []))

(defn delete-message
  [mid user]
  (utl/api-wrap tbot/delete-message (bot) {:chat_id (:user/id user)
                                           :message_id mid}))
