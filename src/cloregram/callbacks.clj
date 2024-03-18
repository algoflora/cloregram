(ns cloregram.callbacks
  (:require [taoensso.timbre :as log]
            [cloregram.db :as db]
            [cloregram.utils :as utl]
            [datomic.api :as d]
            [clojure.edn :as edn]))

(defn ^java.util.UUID create
  ([user ^clojure.lang.Symbol f] (create user f nil))
  ([user ^clojure.lang.Symbol f args]
   (create (java.util.UUID/randomUUID) user f args))
  ([uuid user ^clojure.lang.Symbol f args]
   (let [args (or args {})]
     (d/transact (db/conn) [{:callback/uuid uuid
                             :callback/function f
                             :callback/args (prn-str args)
                             :callback/user [:user/id (:user/id user)]}])
     (log/debug "Created Callback" {:callback-uuid uuid
                                    :callback-function f
                                    :callback-arguments args
                                    :user user})
     uuid)))

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
        args (or (-> callback :callback/args edn/read-string (assoc :user user)) {})]
    (log/debug "Calling Callback function" {:callback-function func
                                            :callback-arguments args
                                            :user user})
    ((utl/resolver func) args)))
