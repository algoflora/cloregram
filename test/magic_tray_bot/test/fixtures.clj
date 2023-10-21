(ns magic-tray-bot.test.fixtures
  (:require  [clojure.java.io :as io]
             [magic-tray-bot.test.infrastructure.core]
             [magic-tray-bot.core :refer [-main]]
             [magic-tray-bot.tasks.update-schema :refer [update-schema]]))

(defn use-test-environment
  [body]
  (-main (io/resource "test-config.edn"))
  (body))

(defn setup-schema
  [body]
  (update-schema ["user"])
  (body))
