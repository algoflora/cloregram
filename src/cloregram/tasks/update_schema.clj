(ns cloregram.tasks.update-schema
  (:require [datomic.api :as d]
            [dialog.logger :as log]
            [cloregram.db :as db]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [cloregram.schema :refer [schema]])
  (:gen-class))

(defn- get-entities-seq
  [dir resource-url]
  (let [resource-file (io/file (.toURI resource-url))]
    (if (.exists resource-file)
      (file-seq resource-file)
      (let [jar-path (str (.getPath resource-url) "!/" dir)
            jar-url (java.net.URL. (str "jar:file:" jar-path))
            jar-conn (.openConnection jar-url)
            jar-stream (.getInputStream jar-conn)
            zip-stream (java.util.zip.ZipInputStream. jar-stream)]
        (loop [entries []]
          (let [entry (.getNextEntry zip-stream)]
            (if entry
              (recur (conj entries entry))
              entries)))))))

(defn- read-dir
  [dir]
  (when-let [resource-url (io/resource dir)]
    (->> resource-url
         (get-entities-seq dir)
         (filter #(= (re-find #"\.[a-zA-Z0-9]+$" (.getName %)) ".edn"))
         (mapcat #(->> % io/reader java.io.PushbackReader. edn/read))
         (vec))))

(defn- read-user-schema
  []
  (read-dir "schema/"))

(defn- read-user-data
  []
  (read-dir "data/"))

(defn update-schema
  "Updates schema with NEW data. Not removing unactual." ; TODO: Develop full update
  []
  (log/info "Updating schema...")
  (let [user-schema (read-user-schema)
        full-schema (concat schema user-schema)
        user-data   (read-user-data)]
    (log/debug "Schema:" full-schema)
    (let [f (d/transact (db/conn) full-schema)]
      (log/info "Schema successfully updated with" (count (:tx-data @f)) "Datoms"))
    (log/debug "Data:" user-data)
    (let [f (d/transact (db/conn) user-data)]
      (log/info "Data successfully updated with" (count (:tx-data @f)) "Datoms"))))

(defn -main
  []
  (update-schema)
  (System/exit 0))
