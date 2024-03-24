(ns cloregram.db
  (:require [datalevin.core :as d]
            [cloregram.system.state :refer [system]]
            [cloregram.utils :as utl]
            [cloregram.schema :refer [schema]]
            [taoensso.timbre :as log]))

(defn conn
  "Returns Datalevin database connection"
  []
  (:db/connection @system))

(defn db
  "Returns current Datalevin database state"
  []
  (let [db (d/db (conn))]
    (log/debug "Received current Datalevin database state")
    db))

(defn- read-project-schema
  []
  (utl/read-resource-dir "schema"))

(defn get-full-schema
  "Updates Datolevin database schema with new entities data. Not removing unactual."
  []
  (log/info "Updating Datalevin database schema...")
  (let [project-schema (read-project-schema)
        full-schema (merge schema project-schema)]
    (log/debug "Datalevin database schema loaded" {:s1 schema
                                                   :s2 project-schema
                                                   :database-schema full-schema})
    full-schema
    #_(let [f (d/transact! (conn) full-schema)]
      (log/debug "Datalevin database schema successfully updated" {:datoms (:tx-data @f)}))))

(defn- read-project-data
  []
  (utl/read-resource-dir "data"))

(defn load-data
  "Loads initial data. Re-write if something exists"
  []
  (log/info "Updating data...")
  (let [user-data (read-project-data)]
    (log/debug "Project data loaded" {:data user-data})
    (let [f (d/transact! (conn) user-data)]
      (log/debug "Project data successfully uploaded" {:datoms (:tx-data @f)}))))
