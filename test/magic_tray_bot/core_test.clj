(ns magic-tray-bot.core-test
  (:require
   [magic-tray-bot.core :refer [-main]]
   [clojure.test :refer :all]))

;; (use-fixtures :each fix/use-in-memory-db fix/use-actual-schema)

(deftest core-test
  (testing "Initialization"
    (-main)))
