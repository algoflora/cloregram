(ns magic-tray-bot.test.infrastructure.users
  (:require [dialog.logger :as log]
            [magic-tray-bot.test.infrastructure.state :as state]))

(defn inc-msg-id
  [uid]
  (swap! state/users #(update-in % [uid :msg-id] inc))
  (uid @state/users))

(defn add ; NOT completely thread safe!
  [uid]
  (let [user {:id (count @state/users)
              :msg-id 0
              :first-name (name uid)
              :last-name nil
              :username (name uid)
              :messages (sorted-map)}]
    (swap! state/users #(assoc % uid user))
    (log/info (format "Added user @%s. Total users count: %d" (name uid) (count @state/users)))
    (log/debug "Users:" @state/users)))
