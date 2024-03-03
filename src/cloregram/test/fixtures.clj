(ns cloregram.test.fixtures
  (:require  [clojure.java.io :as io]
             [cloregram.test.infrastructure.core]
             [cloregram.core :refer [run]]
             [cloregram.db :as db]))

(defn use-test-environment
  [body]
  (run (io/resource "test-config.edn"))
  (body))

(defn load-initial-data
  [body]
  (db/load-data)
  (body))
