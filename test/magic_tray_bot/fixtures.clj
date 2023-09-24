(ns magic-tray-bot.fixtures
  (:require  [clojure.java.io :as io]
             [magic-tray-bot.test.infrastructure.core]
             [magic-tray-bot.core :refer [-main]]))

(defn use-test-environment
  [body]
  (-main (io/resource "test-config.edn"))
  (body))
