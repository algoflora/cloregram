(ns ^:no-doc cloregram.impl.callbacks
  (:require [com.brunobonacci.mulog :as μ]
            [cloregram.database :as db]
            [cloregram.utils :as utl]
            [datalevin.core :as d]
            [clojure.edn :as edn]))

(defn callbacks-count
  []
  (ffirst (d/q '[:find (count ?cb)
                 :where [?cb :callback/uuid]] (db/db))))

(defn ^java.util.UUID create
  ([user ^clojure.lang.Symbol f] (create user f nil))
  ([user ^clojure.lang.Symbol f args]
   (create (java.util.UUID/randomUUID) user f args))
  ([^java.util.UUID uuid user ^clojure.lang.Symbol f args]
   (let [args (or args {})]
     (d/transact! (db/conn) [(cond-> {:callback/uuid uuid
                                      :callback/function f
                                      :callback/arguments args
                                      :callback/user [:user/id (:user/id user)]})])
     (μ/log ::callback-created
            :callback-created/callbacks-count (callbacks-count)
            :callback-created/callback (ffirst
                                        (d/q '[:find (pull ?cb [*])
                                               :in $ ?uuid
                                               :where [?cb :callback/uuid ?uuid]] (db/db) uuid)))
     uuid)))

(defn delete
  [user mid]
  (let [db-ids-to-retract (d/q '[:find ?cb
                          :in $ ?uid ?mid
                          :where
                          [?cb :callback/message-id ?mid]
                          [?cb :callback/user [:user/id ?uid]]]
                        (db/db) (:user/id user) mid)]
    (d/transact! (db/conn) (mapv #(vector :db/retractEntity (first %)) db-ids-to-retract))
    (μ/log ::callbacks-retracted
           :callbacks-retracted/message-id mid
           :callbacks-retracted/retracted-count (count db-ids-to-retract)
           :callbacks-retracted/to-retract db-ids-to-retract
           :callbacks-retracted/callbacks-count (callbacks-count))))

(defn set-new-message-ids
  [mid uuids]
  (let [uuids-to-retract (apply disj (set (mapv first (d/q '[:find ?uuid
                                                       :in $ ?uid ?mid
                                                       :where
                                                       [?cb :callback/user [:user/id ?uid]]
                                                       [?cb :callback/message-id ?mid]
                                                       [?cb :callback/uuid ?uuid]
                                                       #_(not [?cb :callback/uuid ?uuids])] ; TODO: Fix Datalevin with not and collections
                                                     (db/db) (:user/id *current-user*) mid))) (set uuids))]
    (d/transact! (db/conn) (mapv #(vector :db/retractEntity [:callback/uuid %]) uuids-to-retract))
    (d/transact! (db/conn) (mapv #(into {} [[:callback/uuid %] [:callback/message-id mid]]) uuids))
    (μ/log ::set-new-message-ids
           :set-new-message-ids/user *current-user*
           :set-new-message-ids/message-id mid
           :set-new-message-ids/callback-uuids uuids
           :set-new-message-ids/retracted-callbacks-uuids uuids-to-retract
           :set-new-message-ids/final-callbacks-count (callbacks-count))))

(defn- load-callback
  [uuid]
  (let [callback (d/pull (db/db) '[* {:callback/user [*]}] [:callback/uuid uuid])]
    (μ/log ::callback-loaded :callback-loaded/callback callback)
    (when (not= (:user/id *current-user*) (-> callback :callback/user :user/id))
      (throw (ex-info "Wrong User attempt to load Callback!" {:user *current-user* :callback-data callback})))
    callback))

(defn call
  [mid ^java.util.UUID uuid]
  (let [callback (load-callback uuid)
        func (:callback/function callback)
        args (-> callback :callback/arguments (assoc :mid mid))]
    (μ/trace ::callback-call [:user-username (utl/username *current-user*) :function func :arguments args]
             ((utl/resolver func) args))))
