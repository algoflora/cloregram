(ns magic-tray-bot.tasks.update-schema
  (:require [clojure.tools.namespace.find :refer [find-namespaces-in-dir]]
            [datomic.api :as d]
            [dialog.logger :as log]
            [magic-tray-bot.db :as db])
  (:gen-class))

(defn- req&->name!
  [ns]
  (require ns)
  (name ns))

(defn update-schema
  "Updates schema with NEW data. Not removing unactual." ; TODO: Develop full update
  []
  (log/info "Updating schema...")
  (let [schema (->> (find-namespaces-in-dir (clojure.java.io/file "./src/magic_tray_bot/schema"))
                    (map req&->name!)
                    (filter #(some? (resolve (symbol % "schema"))))
                    (mapcat #(load-string (str % "/schema")))
                    (vec))]
    (log/debug "Schema:" schema)
    (let [f (d/transact (db/conn) schema)]
      (log/info "Schema successfully updated with" (count (:tx-data @f)) "Datoms"))))

(defn -main
  []
  (update-schema)
  (System/exit 0))
