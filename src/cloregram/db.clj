(ns cloregram.db
  (:require [datomic.api :as d]
            [cloregram.system.state :refer [system]]
            [cloregram.utils :as utl]
            [cloregram.schema :refer [schema]]
            [dialog.logger :as log]))

(defn conn
  "Returns database connection"
  []
  (:db/connection @system))

(defn db
  "Returns current database state"
  []
  (let [db (d/db (conn))]
    (log/debug "Received current database state:" db)
    db))

(defn- read-user-schema
  []
  (utl/read-resource-dir "schema"))

(defn update-schema
  "Updates schema with new entities data. Not removing unactual."
  []
  (log/info "Updating schema...")
  (let [user-schema (read-user-schema)
        full-schema (concat schema user-schema)]
    (log/debug "Schema:" full-schema)
    (let [f (d/transact (conn) full-schema)]
      (log/info "Schema successfully updated with" (count (:tx-data @f)) "Datoms"))))

(defn- read-user-data
  []
  (utl/read-resource-dir "data"))

(defn load-data
  "Loads initial data. Re-write if something exists"
  []
  (log/info "Updating data...")
  (let [user-data (read-user-data)]
    (log/debug "Data:" user-data)
    (let [f (d/transact (conn) user-data)]
      (log/info "Data successfully updated with" (count (:tx-data @f)) "Datoms"))))
