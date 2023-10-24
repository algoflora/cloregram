(ns cloregram.tasks.update-schema
    (:require [datomic.api :as d]
              [dialog.logger :as log]
              [cloregram.db :as db]
              [clojure.java.io :as io]
              [clojure.edn :as edn])
    (:gen-class))

(defn- read-schema-folder
  [path]
  (when-let [resource (io/resource path)]
    (->> resource
         (.getFile)
         (io/file)
         (file-seq)
         (filter #(= (re-find #"\.[a-zA-Z0-9]+$" (.getName %)) ".edn"))
         (mapcat #(->> % io/reader java.io.PushbackReader. edn/read))
         (vec))))

(defn update-schema
  "Updates schema with NEW data. Not removing unactual." ; TODO: Develop full update
  ([] (update-schema []))
  ([entities]
   (log/info "Updating schema...")
   (let [cloregram-schema (read-schema-folder "cloregram-schema/")
         user-schema (read-schema-folder "schema/")
         schema (concat cloregram-schema user-schema)]
     (log/debug "Schema:" schema)
     (let [f (d/transact (db/conn) schema)]
       (log/info "Schema successfully updated with" (count (:tx-data @f)) "Datoms")))))

(defn -main
  []
  (update-schema)
  (System/exit 0))
