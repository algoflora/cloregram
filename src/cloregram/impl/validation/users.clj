(ns cloregram.impl.validation.users
  (:require [com.brunobonacci.mulog :as μ]
            [cloregram.impl.validation.state :refer [v-users]]))

(defn get-v-user-by-
  [key value]
  (let [v-user (->> @v-users
                    (filter (fn [[k v]] (= value (key v))))
                    (first)
                    (val))]
    (μ/log ::got-virtual-user
           :got-virtual-user/key key
           :got-virtual-user/value value
           :got-virtual-user/virtual-user v-user)
    v-user))

(defn main-message
  [v-user]
  (let [msgs (:messages v-user)
        msg-id (:main-msg-id v-user)]
    (get msgs msg-id)))

(defn- process-temp-messages
  [f v-user]
  (let [ msg-id (:main-msg-id v-user)
        msgs (-> v-user :messages (dissoc msg-id))
        msgs# (filter #(not (contains? (second %) :silent)) msgs)]
    (f msgs#)))

(defn count-temp-messages
  [v-user]
  (process-temp-messages count v-user))

(defn last-temp-message
  [v-user]
  (process-temp-messages #(some-> % last val) v-user))

(defn get-response-or-current
  [vuid func-symbol timeout]
  (let [func     (ns-resolve 'cloregram.impl.validation.users func-symbol)
        trace    (str "get-" func-symbol)
        v-user   (vuid @v-users)
        interval 100]
    (μ/trace (keyword "cloregram.impl.validation.users" trace)
             {:pairs [(keyword trace "virtual-user") v-user
                      (keyword trace "timeout") timeout]
              :capture (fn [resp] {(keyword trace "response") resp})}
             (loop [t timeout
                    vu v-user]
               (cond (not (:waiting-for-response? vu))
                     (func vu)

                     (= 0 t) (throw (ex-info (format "No %s!" func-symbol) {:timeout timeout}))

                     :else (do (Thread/sleep interval)
                               (recur (- t interval) (vuid @v-users))))))))

(defn add-v-user
  [vuid]
  (let [v-user {:id (inc (count @v-users))
                :msg-id 0
                :main-msg-id nil
                :first-name (name vuid)
                :last-name nil
                :username (name vuid)
                :language-code "en"
                :messages (sorted-map)
                :waiting-for-response? false}]
    (swap! v-users #(assoc % vuid v-user))
    (μ/log ::add-virtual-user
           :add-virtual-user/virtual-user (vuid @v-users)
           :add-virtual-user/virtual-users-count (count @v-users))))
