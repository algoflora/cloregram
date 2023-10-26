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
              :main-msg-id nil
              :first-name (name uid)
              :last-name nil
              :username (name uid)
              :language-code "en"
              :messages (sorted-map)}]
    (swap! state/users #(assoc % uid user))
    (log/infof "Added user @%s. Total users count: %d" (name uid) (count @state/users))
    (log/debug "Users:" @state/users)))

(defn- get-user-by-
  [key value]
  (log/debugf "Getting User by %s -> %s" key value)
  (let [user (->> @state/users
                  (filter (fn [[k v]] (= value (key v))))
                  (first)
                  (val))]
    (log/debug "Got User" user)
    user))

(defn get-user-by-id
  [id] (get-user-by- :id id))

(defn get-user-by-uid
  [uid] (get-user-by- :username (name uid)))

(defn- get-current-main-message
  [uid]
  (let [user (uid @state/users)
        msgs (:messages user)
        msg-id (:msg-id user)]
    (msgs msg-id)))

(defn wait-main-message
  ([uid] (wait-main-message uid 1000))
  ([uid timeout]
   (log/infof "Waiting main message for User %s (timeout: %d)" uid timeout)
   (let [interval 100
         current-main-message (get-current-main-message uid)]
     (log/debug "Current main message:" current-main-message)
     (loop [t (- timeout interval)]
       (let [new-main-message (get-current-main-message uid)]
         (cond (not= current-main-message new-main-message)
               (do (log/infof "User %s got new main Message: %s" uid (utl/msg->str new-main-message))
                   new-main-message)
               
               (= 0 t) (throw (ex-info "No new main Message!" {:timeout timeout}))
               
               :else (do (Thread/sleep interval)
                         (recur (- t interval)))))))))
