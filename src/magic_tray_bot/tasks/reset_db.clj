(ns magic-tray-bot.tasks.reset-db
  (:require [datomic.api :as d]
            [dialog.logger :as log]
            [magic-tray-bot.db :as db]
            [magic-tray-bot.tasks.update-schema :refer [update-schema]]
            [nano-id.core :refer [nano-id]])
  (:gen-class))

(defn- delete-db
  []
  (d/delete-database db/uri)
  (log/info "Database deleted if it was exist"))

(defn- create-db
  []
  (d/create-database db/uri)
  (d/gc-storage (db/conn) (java.util.Date.))
  (log/info "Database created"))

(defn- reset-db
  []
  (delete-db)
  (create-db)
  (update-schema))

(defn -main
  "Completely wipes database and write new schema from magic-tray-bot.schema.*"
  []
  (let [code (nano-id 5)]
    (println (str "All data in database will be vanished! If you are sure, then type \"" code "\" below:"))
    (if (= code (read-line))
      (reset-db)
      (println "Database reset cancelled. Good bye!"))
    (System/exit 0)))
