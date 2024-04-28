(ns ^:no-doc cloregram.impl.database
  (:require [cloregram.impl.schema :refer [schema]]
            [cloregram.impl.resources :refer [read-resource-dir]]))

(defn- read-project-schema
  []
  (read-resource-dir "schema"))

(defn get-full-schema
  "Reads Datalevin database schema."
  []
  (let [project-schema (read-project-schema)]
    (merge schema project-schema)))

(defn read-project-data
  []
  (read-resource-dir "data"))
