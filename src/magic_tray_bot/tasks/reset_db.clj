(ns magic-tray-bot.tasks.reset-db
  (:require [datomic.api :as d]
            [dialog.logger :as log]
            [magic-tray-bot.db :as db]

            [magic-tray-bot.schema.product]
            [magic-tray-bot.schema.project]
            [magic-tray-bot.schema.user])
  (:gen-class))

(defn- delete-db
  []
  (d/delete-database db/uri)
  (log/info "Database deleted"))

(defn- create-db
  []
  (d/create-database db/uri)
  (d/gc-storage (db/conn) (java.util.Date.))
  (log/info "Database created"))

(defn pr
  "Print + Return"
  [x]
  (log/info x)
  x)

(defn- fill-up-schema
  []
  (let [schema (->> (all-ns)
                    (map ns-name)
                    (filter #(clojure.string/starts-with? % "magic-tray-bot.schema"))
                    (pr)
                    (mapcat #(load-string (str % "/schema")))
                    (vec))]
    (log/debug "Schema to load:" schema)
    (d/transact (db/conn) schema)))

(defn -main
  "Completely wipes database and write new schema from magic-tray-bot.schema.*"
  []
  (delete-db)
  (create-db)
  (if (fill-up-schema)
    (log/info "Schema loaded")
    (log/error "Something goes wrong!")))
