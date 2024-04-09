(ns cloregram.impl.callbacks
  (:require [com.brunobonacci.mulog :as μ]
            [cloregram.database :as db]
            [cloregram.utils :as utl]
            [datalevin.core :as d]
            [clojure.edn :as edn]))

(defn ^java.util.UUID create
  ([user ^clojure.lang.Symbol f] (create user f nil))
  ([user ^clojure.lang.Symbol f args]
   (create (java.util.UUID/randomUUID) user f args))
  ([^java.util.UUID uuid user ^clojure.lang.Symbol f args]
   (create uuid nil user f args))
  ([^java.util.UUID uuid msg-id user ^clojure.lang.Symbol f args]
   (let [args (or args {})]
     (d/transact! (db/conn) [(cond-> {:callback/uuid uuid
                                      :callback/function f
                                      :callback/arguments args
                                      :callback/user [:user/id (:user/id user)]}
                               (some? msg-id) (assoc :callback/message-id msg-id))])
     (μ/log ::callback-created
            :callback-created/callbacks-count (ffirst
                                               (d/q '[:find (count ?cb)
                                                      :where [?cb :callback/uuid]] (db/db)))
            :callback-created/callback (ffirst
                                        (d/q '[:find (pull ?cb [*])
                                               :in $ ?uuid
                                               :where [?cb :callback/uuid ?uuid]] (db/db) uuid)))
     uuid)))

(defn delete
  [user mid]
  (let [to-retract (d/q '[:find ?cb
                          :in $ ?uid ?mid
                          :where
                          [?cb :callback/message-id ?mid]
                          [?cb :callback/user [:user/id ?uid]]]
                        (db/db) (:user/id user) mid)]
    (d/transact! (db/conn) (mapv #(vector :db/retractEntity (first %)) to-retract))
    (μ/log ::callbacks-retracted
           :callbacks-retracted/message-id mid
           :callbacks-retracted/retracted-count (count to-retract)
           :callbacks-retracted/to-retract to-retract
           :callbacks-retracted/callbacks-count (ffirst
                                                 (d/q '[:find (count ?cb)
                                                        :where [?cb :callback/uuid]] (db/db)))
           :callbacks-retracted/callbacks (d/q '[:find (pull ?cb [*])
                                                 :where
                                                 [?cb :callback/uuid]] (db/db)))))

(defn set-new-message-ids
  [user mid uuids]
  (let [explain (d/explain {:run? true}
                           '[:find ?cb
                             :in $ ?uid ?mid [?uuids ...]
                             :where
                             [?cb :callback/user [:user/id ?uid]]
                             [?cb :callback/message-id ?mid]
                             (not [?cb :callback/uuid ?uuids])]
                           (db/db) (:user/id user) mid uuids)
        callbacks  (d/q '[:find (pull ?cb [*])
                                 :where
                                 [?cb :callback/uuid]] (db/db))
        to-retract (d/q '[:find ?cb
                          :in $ ?uid ?mid [?uuids ...]
                          :where
                          [?cb :callback/user [:user/id ?uid]]
                          [?cb :callback/message-id ?mid]
                          (not [?cb :callback/uuid ?uuids])]
                        (db/db) (:user/id user) mid uuids)]
    (d/transact! (db/conn) (mapv #(vector :db/retractEntity (first %)) to-retract))
    (d/transact! (db/conn) (mapv #(into {} [[:callback/uuid %] [:callback/message-id mid]]) uuids))
    (μ/log ::callbacks-set-message-id
           :callbacks-set-message-id/explain explain
           :callbacks-set-message-id/uuids uuids
           :callbacks-set-message-id/message-id mid
           :callbacks-set-message-id/retracted-count (count to-retract)
           :callbacks-set-message-id/to-retract to-retract
           :callbacks-set-message-id/callbacks-count (ffirst
                                                      (d/q '[:find (count ?cb)
                                                             :where [?cb :callback/uuid]] (db/db)))
           :callbacks-set-message-id/callbacks callbacks)))

(defn- load-callback
  [user uuid]
  (let [callback (d/pull (db/db) '[* {:callback/user [*]}] [:callback/uuid uuid])]
    (μ/log ::callback-loaded [:callback-loaded/uuid uuid
                              :callback-loaded/data callback])
    (when (not= (:user/id user) (-> callback :callback/user :user/id))
      (throw (ex-info "Wrong User attempt to load Callback!" {:user user :callback-data callback})))
    callback))

(defn call
  [user ^java.util.UUID uuid]
  (let [callback (load-callback user uuid)
        func (:callback/function callback)
        args (-> callback :callback/arguments (assoc :user user))]
    (μ/trace ::callback-call [:user-username (utl/username user) :function func :arguments args]
             ((utl/resolver func) args))))
