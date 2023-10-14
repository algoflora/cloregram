(ns magic-tray-bot.test.infrastructure.users
  (:require [dialog.logger :as log]
            [magic-tray-bot.test.infrastructure.state :as state]))

(defn inc-msg-id
  [uid]
  (swap! state/users #(update-in % [uid :msg-id] inc))
  (uid @state/users))

(defn add ; NOT completely thread safe!
  [uid]
  (let [user {:id (+ 10000000 (count @state/users))
              :msg-id 1
              :first-name (name uid)
              :last-name nil
              :username (name uid)
              :messages (sorted-map)}]
    (swap! state/users #(assoc % uid user))
    (log/info (format "Added user @%s. Total users count: %d" (name uid) (count @state/users)))
    (log/debug "Users:" @state/users)))

(defn get-uid-by-id
  [id]
  (->> @state/users
       (filter (fn [[k v]] (= id (:id v))))
       (first)
       (key)))

(defn wait-for-new-message
  ([uid] (wait-for-new-message uid 10000))
  ([uid timeout]
   (log/info (format "Waiting message for User %s (timeout: %d)" uid timeout) (uid @state/users))
   (let [interval 100
         current-messages-count (-> @state/users uid :messages count)]
     (log/debug "Current messages count:" current-messages-count)
     (loop [t (- timeout interval)]
       (cond (< current-messages-count (-> @state/users uid :messages count))
             (let [new-message (-> @state/users uid :messages last val)]
               (log/info (format "User %s got new Message: %s" uid new-message))
               new-message)

             (= 0 t) (throw (ex-info "No new messages!" {:timeout timeout}))

             :else (do (Thread/sleep interval)
                       (recur (- t interval))))))))
