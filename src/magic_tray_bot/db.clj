(ns magic-tray-bot.db
  (:require [datomic.api :as d]
            [magic-tray-bot.system.state :refer [system]]
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

