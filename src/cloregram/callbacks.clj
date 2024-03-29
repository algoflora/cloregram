(ns cloregram.callbacks
  (:require [clojure.tools.logging :as log]
            [com.brunobonacci.mulog :as μ]
            [cloregram.db :as db]
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
     (log/debug "Created Callback" {:callback-uuid uuid
                                    :callback-function f
                                    :callback-arguments args
                                    :callback-message-id msg-id
                                    :user user})
     uuid)))

(defn delete
  [user mid]
  (let [to-retract (d/q '[:find ?cb
                          :in $ ?uid ?mid
                          :where
                          [?cb :callback/message-id ?mid]
                          [?cb :callback/user ?user]
                          [?user :user/id ?uid]]
                        (db/db) (:user/id user) mid)]
    (d/transact! (db/conn) (mapv #(vector :db/retractEntity (first %)) to-retract))
    (log/debug "Retracted Callbacks" {:callback-message-id mid
                                      :retracted-count (count to-retract)
                                      :to-retract to-retract
                                      :callbacks-count (ffirst
                                                        (d/q '[:find (count ?cb)
                                                               :where [?cb :callback/uuid]] (db/db)))
                                      
                                      :callbacks (d/q '[:find (pull ?cb [*]) ?uname
                                                        :where
                                                        [?cb :callback/user ?u]
                                                        [?u :user/username ?uname]] (db/db))})))

(defn set-new-message-ids
  [user mid uuids]
  (let [to-retract (into [] (d/q '[:find ?cb
                                   :in $ ?uid ?mid ?uuids
                                   :where
                                   [?cb :callback/message-id ?mid]
                                   [?cb :callback/user ?user]
                                   [?user :user/id ?uid]
                                   [?cd :callback/uuid ?uuid]
                                   (not [(contains? ?uuids ?uuid)])]
                                 (db/db) (:user/id user) mid (set uuids)))]
    (d/transact! (db/conn) (mapv #(vector :db/retractEntity (first %)) to-retract))
    (d/transact! (db/conn) (mapv #(into {} [[:callback/uuid %] [:callback/message-id mid]]) uuids))
    (log/debug "Callback Message IDs are set" {:callback-uuids uuids
                                               :callback-message-id mid
                                               :retracted-count (count to-retract)
                                               :to-retract to-retract
                                               :callbacks-count (ffirst
                                                                 (d/q '[:find (count ?cb)
                                                                        :where [?cb :callback/uuid]] (db/db)))
                                               :callbacks (d/q '[:find (pull ?cb [*]) ?uname
                                                                 :where
                                                                 [?cb :callback/user ?u]
                                                                 [?u :user/username ?uname]] (db/db))})))

(defn- load-callback
  [user uuid]
  (let [callback (d/pull (db/db) '[* {:callback/user [*]}] [:callback/uuid uuid])]
    (log/debug "Loaded callback" {:callback-uuid uuid
                                  :callback-data callback})
    (when (not= (:id user) (-> callback :callback/user :id))
      (throw (ex-info "Wrong User attempt to load Callback!" {:user user :callback-data callback})))
    callback))

(defn call
  [user ^java.util.UUID uuid]
  (let [callback (load-callback user uuid)
        func (:callback/function callback)
        args (-> callback :callback/arguments (assoc :user user))]
    (log/debug "Calling Callback function" {:callback-function func
                                            :callback-arguments args})
    (μ/trace ::callback-call [:user-username (utl/username user) :function func :arguments args]
             ((utl/resolver func) args))))
