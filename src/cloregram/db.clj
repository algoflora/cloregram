(ns cloregram.db
  (:require [datomic.api :as d]
            [cloregram.system.state :refer [system]]
            [cloregram.utils :as utl]
            [cloregram.schema :refer [schema]]
            [taoensso.timbre :as log]))

(defn conn
  "Returns Datiomic database connection"
  []
  (:db/connection @system))

(defn db
  "Returns current Datomic database state"
  []
  (let [db (d/db (conn))]
    (log/debug "Received current Datomic database state" {:database-state db})
    db))

(defn- read-project-schema
  []
  (utl/read-resource-dir "schema"))

(defn update-schema
  "Updates schema with new entities data. Not removing unactual."
  []
  (log/info "Updating schema...")
  (let [project-schema (read-project-schema)
        full-schema (concat schema project-schema)]
    (log/debug "Schema" full-schema)
    (let [f (d/transact (conn) full-schema)]
      (log/debug "Schema successfully updated" {:datoms (:tx-data @f)}))))

(defn- read-project-data
  []
  (utl/read-resource-dir "data"))

(defn load-data
  "Loads initial data. Re-write if something exists"
  []
  (log/info "Updating data...")
  (let [user-data (read-project-data)]
    (log/debug "Readed project data" user-data)
    (let [f (d/transact (conn) user-data)]
      (log/debug "Data successfully updated" {:datoms (:tx-data @f)}))))
