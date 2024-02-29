(ns cloregram.test.infrastructure.users
  (:require [dialog.logger :as log]
            [cloregram.test.infrastructure.state :as state]
            [cloregram.utils :as utl]))

(defn inc-msg-id
  [uid]
  (swap! state/users #(update-in % [uid :msg-id] inc))
  (uid @state/users))

(defn add
  [uid]
  (let [user {:id (inc (count @state/users))
              :msg-id 0
              :main-msg-id nil
              :first-name (name uid)
              :last-name nil
              :username (name uid)
              :language-code "en"
              :messages (sorted-map)
              :waiting-for-response? false}]
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

(defn get-user-info
  [users msg]
  (let [user (->> users
                  (filter (fn [[k v]] (= (:chat_id msg) (:id v))))
                  (first)
                  (val))
        uid (-> user :username keyword)]
     [user uid]))

(defn get-user-by-id
  [id] (get-user-by- :id id))

(defn get-user-by-uid
  [uid] (get-user-by- :username (name uid)))

(defn- main-message#
  [uid]
  (let [user (uid @state/users)
        msgs (:messages user)
        msg-id (:main-msg-id user)]
    (msgs msg-id)))

(defn- count-temp-messages#
  [uid]
  (let [user (uid @state/users)
        msg-id (:main-msg-id user)
        msgs (-> user :messages (dissoc msg-id))
        msgs# (filter #(not (contains? (second %) :silent)) msgs)]
    (count msgs#)))

(defn- last-temp-message#
  [uid]
  (log/debug (uid @state/users))
  (let [user (uid @state/users)
        msg-id (:main-msg-id user)
        msgs (-> user :messages (dissoc msg-id))
        msgs# (filter #(not (contains? (second %) :silent)) msgs)]
    (-> msgs# last val)))

(defn- get-response-or-current
  [uid f s timeout]
  (log/infof "Getting %s for User %s (timeout: %d)" s uid timeout)
  (let [interval 100]
    (loop [t timeout]
      (cond (not (:waiting-for-response? (uid @state/users)))
            (let [resp (f uid)]
              (log/infof "User %s got %s: %s" uid s resp)
              resp)

            (= 0 t) (throw (ex-info (format "No %s!" s) {:timeout timeout}))

            :else (do (Thread/sleep interval)
                      (recur (- t interval)))))))

(defn main-message
  ([uid] (main-message uid 2000))
  ([uid timeout]
   (get-response-or-current uid main-message# "main Message" timeout)))

(defn last-temp-message
  ([uid] (last-temp-message uid 2000))
  ([uid timeout]
   (get-response-or-current uid last-temp-message# "temp Message" timeout)))

(defn count-temp-messages
  ([uid] (count-temp-messages  uid 2000))
  ([uid timeout]
   (get-response-or-current uid count-temp-messages# "temp Messages count" timeout)))




