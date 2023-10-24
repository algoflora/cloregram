(ns cloregram.tasks.update-schema
    (:require [datomic.api :as d]
              [dialog.logger :as log]
              [cloregram.db :as db]
              [clojure.java.io :as io]
              [clojure.edn :as edn])
    (:gen-class))

(defn update-schema
  "Updates schema with NEW data. Not removing unactual." ; TODO: Develop full update
  ([] (update-schema []))
  ([entities]
   (log/info "Updating schema...")
   (let [transf #(->> % (io/reader) (java.io.PushbackReader.) (edn/read))
         schema (->> (file-seq (io/file "./src/cloregram/schema"))
                     (filter #(= (re-find #"\.[a-zA-Z0-9]+$" (.getName %)) ".edn"))
                     (filter (if (empty? entities) true #(some #{(second (re-find #"^([a-zA-Z0-9\-]+)\.edn" (.getName %)))} entities)))
                     (mapcat transf)
                     (vec))]
     (log/debug "Schema:" schema)
     (let [f (d/transact (db/conn) schema)]
       (log/info "Schema successfully updated with" (count (:tx-data @f)) "Datoms")))))

(defn -main
  []
  (update-schema)
  (System/exit 0))
