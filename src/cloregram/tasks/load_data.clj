(ns cloregram.tasks.load-data
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

(defn- read-user-data
  []
  (read-dir "data"))

(defn load-data
  "Loads initial data. Re-write if something exists"
  []
  (log/info "Updating data...")
  (let [user-data (read-user-data)]
    (log/debug "Data:" user-data)
    (let [f (d/transact (db/conn) user-data)]
      (log/info "Data successfully updated with" (count (:tx-data @f)) "Datoms"))))

(defn -main
  []
  (load-data)
  (System/exit 0))
