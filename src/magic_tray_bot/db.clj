(ns magic-tray-bot.db
  (:require [datomic.api :as d]
            [dialog.logger :as log]))

(def ^:private db-uri "datomic:dev://localhost:4334/magic-tray-dev")

(def conn
  "Database connection"
  (d/connect db-uri))

(defn get-db
  "Returns current database state"
  []
  (let [db (d/db conn)]
    (log/debug "Received current database state:" db)
    db))

