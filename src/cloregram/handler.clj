(ns cloregram.handler
  (:require [cloregram.system.state :refer [bot system]]
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
  [{:keys [user message]}]
  (utl/api-wrap tbot/send-message (bot)
            {:chat_id (:user/id user)
             :text (str (:user/username user) " " (str/upper-case (:text message)))
             :reply_markup [[{:text "+"
                              :callback_query (str (clb/create user 'cloregram.handler/increment [0]))}
                             {:text "-"
                              :callback_query (str (clb/create user 'cloregram.handler/decrement [0]))}]]}))

(defn increment
  [n user]
  (let [n (inc n)]
    (utl/api-wrap tbot/send-message (bot)
              {:chat_id (:user/id user)
               :text (format "Incremented: %d" n)
               :reply_markup [[{:text "+"
                                :callback_query (str (clb/create user 'cloregram.handler/increment [n]))}
                               {:text "-"
                                :callback_query (str (clb/create user 'cloregram.handler/decrement [n]))}]]})))

(defn decrement
  [n user]
  (let [n (dec n)]
    (utl/api-wrap tbot/send-message (bot)
              {:chat_id (:user/id user)
               :text (format "Decremented: %d" n)
               :reply_markup [[{:text "+"
                                :callback_query (str (clb/create user 'cloregram.handler/increment [n]))}
                               {:text "-"
                                :callback_query (str (clb/create user 'cloregram.handler/decrement [n]))}]]})))

