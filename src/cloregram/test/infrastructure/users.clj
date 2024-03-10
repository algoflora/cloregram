(ns cloregram.test.infrastructure.users
  (:require [taoensso.timbre :as log]
            [cloregram.test.infrastructure.state :as state]
            [cloregram.utils :as utl]))

(defn add
  "Creates virtual user with username and first-name `uid`, language-code 'en' and empty mesages storage. Writes this user into test infrastructure virtual users state storage with key `uid`."
  {:changed "0.8"}
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

(defn get-user-by-uid
  "Returns virtual user structure by key `uid` from test infrastructure virtual users state storage"
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
  (let [user (uid @state/users)
        msg-id (:main-msg-id user)
        msgs (-> user :messages (dissoc msg-id))
        msgs# (filter #(not (contains? (second %) :silent)) msgs)]
    (some-> msgs# last val)))

(defn- get-response-or-current
  [uid f s timeout]
  (log/infof "Getting %s for User %s (timeout: %d)" s uid timeout)
  (let [interval 100]
    (loop [t timeout]
      (cond (not (:waiting-for-response? (uid @state/users)))
            (let [resp (f uid)]
              (log/info (format "User %s got %s" uid s) {s resp})
              resp)

            (= 0 t) (throw (ex-info (format "No %s!" s) {:timeout timeout}))

            :else (do (Thread/sleep interval)
                      (recur (- t interval)))))))

(defn main-message
  "Returns main message of virtual user with key `uid` from test infrastructure virtual users state storage. If virtual user is not awaiting response, then message structure or nil is returned immidiately. If virtual user is awaiting response, then function will wait until response or `timeout` milliseconds (default 2000). If awaiting of response not finished while timeout passed, Exception will be throwed."
  {:added "0.8"}
  ([uid] (main-message uid 2000))
  ([uid timeout]
   (get-response-or-current uid main-message# "main Message" timeout)))

(defn last-temp-message
  "Returns last temporal message of virtual user with key `uid` from test infrastructure virtual users state storage. If virtual user is not awaiting response, then temporal message structure or nil is returned immidiately. If virtual user is awaiting response, then function will wait until response or `timeout` milliseconds (default 2000). If awaiting of response not finished while timeout passed, Exception will be throwed."
  {:added "0.8"}
  ([uid] (last-temp-message uid 2000))
  ([uid timeout]
   (get-response-or-current uid last-temp-message# "temp Message" timeout)))

(defn count-temp-messages
  "Returns count of temporal messages of virtual user with key `uid` from test infrastructure virtual users state storage. If virtual user is not awaiting response, then temporal messages count is returned immidiately. If virtual user is awaiting response, then function will wait until response or `timeout` milliseconds (default 2000). If awaiting of response not finished while timeout passed, Exception will be throwed."
  {:added "0.8"}
  ([uid] (count-temp-messages  uid 2000))
  ([uid timeout]
   (get-response-or-current uid count-temp-messages# "temp Messages count" timeout)))




