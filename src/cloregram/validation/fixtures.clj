(ns cloregram.validation.fixtures
  (:require [cloregram.core :refer [run set-exceptions-handler]]
            [cloregram.database :as db]
            [clojure.java.io :as io]))

(defn use-test-environment
  [body]
  (set-exceptions-handler)
  (run (io/resource "test-config.edn"))
  (body))

(defn load-initial-data
  [body]
  (db/load-data)
  (body))
