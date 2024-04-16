(ns cloregram.impl.database
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [resauce.core :as res]
            [cloregram.impl.schema :refer [schema]]))

(defn- read-resource [resource-url]
  (with-open [stream (io/input-stream resource-url)]
    (-> stream
        io/reader
        java.io.PushbackReader. edn/read)))

(defn- read-resource-dir
  [dir]
  (when-let [resources (some-> dir io/resource res/url-dir)]
    (->> resources
         (filter #(str/ends-with? % ".edn"))
         (mapcat read-resource))))

(defn- read-project-schema
  []
  (read-resource-dir "schema"))

(defn ^:no-doc get-full-schema
  "Reads Datalevin database schema."
  []
  (let [project-schema (read-project-schema)]
    (merge schema project-schema)))

(defn read-project-data
  []
  (read-resource-dir "data"))
