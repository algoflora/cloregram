(ns magic-tray-bot.core-test
  (:require [clojure.test :refer :all]
            [magic-tray-bot.test.fixtures :as fix]
            [magic-tray-bot.test.infrastructure.users :as users]
            [magic-tray-bot.test.infrastructure.client :as client]
            [dialog.logger :as log]))

(use-fixtures :once fix/use-test-environment)

(deftest core-test
  (testing "Initialization"
    (log/info "Testing started")
    (users/add :testuser-1)
    (client/send-text :testuser-1 "Hello, bot!")
    (is true)
    (log/info "Testing finished")))
