(ns magic-tray-bot.core-test
  (:require [clojure.test :refer :all]
            [magic-tray-bot.fixtures :as fix]
            [dialog.logger :as log]))

(use-fixtures :once fix/use-test-environment)

(deftest core-test
  (testing "Initialization"
    (Thread/sleep 10)))
