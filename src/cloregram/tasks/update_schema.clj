(ns cloregram.tasks.update-schema
  (:require [datomic.api :as d]
            [dialog.logger :as log]
            [cloregram.db :as db]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [cloregram.schema :refer [schema]])
  (:gen-class))

(defn- read-user-schema
  []
  (when-let [resource (io/resource "schema/")]
    (->> resource
         (.getFile)
         (io/file)
         (file-seq)
         (filter #(= (re-find #"\.[a-zA-Z0-9]+$" (.getName %)) ".edn"))
         (mapcat #(->> % io/reader java.io.PushbackReader. edn/read))
         (vec))))

(defn update-schema
  "Updates schema with NEW data. Not removing unactual." ; TODO: Develop full update
  []
  (log/info "Updating schema...")
  (let [user-schema (read-user-schema)
        full-schema (concat schema user-schema)]
    (log/debug "Schema:" full-schema)
    (let [f (d/transact (db/conn) full-schema)]
      (log/info "Schema successfully updated with" (count (:tx-data @f)) "Datoms"))))

(defn -main
  []
  (update-schema)
  (System/exit 0))
