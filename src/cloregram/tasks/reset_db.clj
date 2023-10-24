(ns cloregram.tasks.reset-db
  (:require [datomic.api :as d]
            [dialog.logger :as log]
            [cloregram.db :as db]
            [cloregram.tasks.update-schema :refer [update-schema]]
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
  "Completely wipes database and write new schema from cloregram.schema.*"
  []
  (let [code (nano-id 5)]
    (println (str "All data in database '" db/uri "' will be vanished! If you are sure, then type \"" code "\" below:"))
    (if (= code (read-line))
      (reset-db)
      (println "Database reset cancelled. Good bye!"))
    (System/exit 0)))
