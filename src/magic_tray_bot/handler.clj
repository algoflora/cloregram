(ns magic-tray-bot.handler
  (:require [magic-tray-bot.system.state :refer [bot]]
            [magic-tray-bot.users :as u]
            [magic-tray-bot.utils :refer [api-wrap]]
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
        _ (log/debug "HANDLER TYPE:" (type hdata))
        handler (-> hdata first resolve)
        args (-> hdata second edn/read-string (assoc :user user :message msg))]
    (log/info "Handling message" msg "from User" user) ; TODO: info?
    (log/debug (format "Calling %s with args %s" handler args))
    (handler args)))

(defn common
  [{:keys [num user message]}]
  (api-wrap tbot/send-message (bot) {:chat_id (:user/id user)
                                     :text (str num " " (:user/username user) " " (str/upper-case (:text message)))}))
