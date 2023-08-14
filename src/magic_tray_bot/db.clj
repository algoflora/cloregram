(ns magic-tray-bot.db
  (:require [datomic.api :as d]
            [dialog.logger :as log]))

(def uri "datomic:dev://localhost:4334/magic-tray-dev")

(def ^:private connection (atom nil))

(defn conn
  "Database connection"
  []
  @connection)

(defn init-conn
  "Initialize new database connection"
  []
  (reset! connection (d/connect uri)))

(defn get-db
  "Returns current database state"
  []
  (let [db (d/db (conn))]
    (log/debug "Received current database state:" db)
    db))

(init-conn)

