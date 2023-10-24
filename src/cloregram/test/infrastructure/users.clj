(ns cloregram.test.infrastructure.users
  (:require [dialog.logger :as log]
            [cloregram.test.infrastructure.state :as state]
            [cloregram.utils :as utl]))

(defn inc-msg-id
  [uid]
  (swap! state/users #(update-in % [uid :msg-id] inc))
  (uid @state/users))

(defn add ; TODO: NOT completely thread safe!
  [uid]
  (let [user {:id (inc (count @state/users))
              :msg-id 1
              :first-name (name uid)
              :last-name nil
              :username (name uid)
              :language-code "en"
              :messages (sorted-map)}]
    (swap! state/users #(assoc % uid user))
    (log/infof "Added user @%s. Total users count: %d" (name uid) (count @state/users))
    (log/debug "Users:" @state/users)))

(defn get-uid-by-id
  [id]
  (->> @state/users
       (filter (fn [[k v]] (= id (:id v))))
       (first)
       (key)))

(defn wait-for-new-message
  ([uid] (wait-for-new-message uid 1000))
  ([uid timeout]
   (log/infof "Waiting message for User %s (timeout: %d)" uid timeout)
   (let [interval 100
         current-messages-count (-> @state/users uid :messages count)]
     (log/debug "Current messages count:" current-messages-count)
     (loop [t (- timeout interval)]
       (cond (< current-messages-count (-> @state/users uid :messages count))
             (let [new-message (-> @state/users uid :messages last val)]
               (log/infof "User %s got new Message: %s" uid (utl/msg->str new-message))
               new-message)

             (= 0 t) (throw (ex-info "No new messages!" {:timeout timeout}))

             :else (do (Thread/sleep interval)
                       (recur (- t interval))))))))
