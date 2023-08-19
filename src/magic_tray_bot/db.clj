(ns magic-tray-bot.db
  (:require [datomic.api :as d]
            [dialog.logger :as log]))

(def uri "datomic:dev://localhost:4334/magic-tray-dev")

(def ^:private connection (atom nil))

(defn- init-conn
  "Initialize new database connection"
  []
  (reset! connection (d/connect uri))
  (log/debug "Database connection initialized for " uri))

(defn conn
  "Database connection"
  []
  (if-let [c @connection]
    c
    (do (init-conn)
        (conn))))

(defn get-db
  "Returns current database state"
  []
  (let [db (d/db (conn))]
    (log/debug "Received current database state:" db)
    db))

