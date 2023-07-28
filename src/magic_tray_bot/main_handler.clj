(ns magic-tray-bot.main-handler
  (:require [dialog.logger :as log]
            [cheshire.core :refer [generate-string]]
            [magic-tray-bot.user :as user]))

(defn- handle-user
  "Returns current user information. If no such user then creates it first"
  [msg]
  (let [from (get-in msg [:message :from])
        uname-from (:username from)
        uinfo (user/get-by-username uname-from)]
    (log/debug (str "User:\t" uinfo))
    (cond
      (empty? uinfo)
      (do (log/debug (str "Creating new user @" uname-from))
          (user/create! from))

      (user/compare-info from uinfo)
      (do (log/debug (str "User @" uname-from " unchanged"))
          uinfo)

      :else
      (do
        (log/debug (str "Updating user @" uname-from))
        (user/update-info! uname-from from)))))

(defn handle
  "Main handling function"
  [bot msg]
  (let [msg-str (generate-string msg {:pretty true})]
    (log/debug (str "Incoming message:\n" msg-str))
    (let [from (get-in msg [:message :from])
          chat (get-in msg [:message :chat])]
      (cond
        (:is_bot from)
        (do
          (log/warn (str "Message from bot!\t" from))
          false)

        (not= "private" (:type chat))
        (do
          (log/warn (str "Message from non-private chat!\t" chat))
          false)

        :else
        (let [uinfo (handle-user msg)]
          (log/debug (str "User:\t"  (generate-string uinfo {:pretty true}))))))))
