(ns cloregram.tasks.update-schema
  (:require [datomic.api :as d]
            [dialog.logger :as log]
            [cloregram.db :as db]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [cloregram.schema :refer [schema]]
            [cloregram.utils :as utl])
  (:import [java.nio.file Files FileSystems FileVisitOption LinkOption OpenOption]
           [java.lang String])
  (:gen-class))

(defn- empty-arr
  [class]
  (make-array class 0))

(defn- reduce-fn-jar
  [acc ^java.nio.file.Path p]
  (if (and (Files/isRegularFile p (empty-arr LinkOption)) (str/ends-with? (str p) ".edn"))
    (with-open [inputStream (Files/newInputStream p (empty-arr OpenOption))]
      (apply conj acc (-> inputStream io/reader java.io.PushbackReader. edn/read)))
    acc))

(defn- reduce-fn-fs
  [acc ^java.io.File f]
  (if (and (.isFile f) (str/ends-with? (.getName f) ".edn"))
    (apply conj acc (-> f slurp edn/read-string))
    acc))

(defn- read-dir
  [dir]
  (when-let [resource-uri (some-> dir io/resource .toURI)]
    (if (= (.getScheme resource-uri) "jar")
      (let [env (doto (java.util.HashMap.) (.put "create" "false"))
            fs (FileSystems/newFileSystem resource-uri env)]
        (try
          (let [root (.getPath fs dir (empty-arr String))
                paths (-> root (Files/walk (empty-arr FileVisitOption)) .iterator iterator-seq)]
            (reduce reduce-fn-jar [] paths))
          (finally
            (.close fs))))
      (->> (io/file resource-uri)
           (file-seq)
           (reduce reduce-fn-fs [])))))

(defn- read-user-schema
  []
  (read-dir "schema"))

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
