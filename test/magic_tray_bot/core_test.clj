(ns magic-tray-bot.core-test
  (:require [clojure.test :refer :all]
            [magic-tray-bot.test.fixtures :as fix]
            [magic-tray-bot.test.infrastructure.users :as users]
            [magic-tray-bot.test.infrastructure.client :as client]
            [dialog.logger :as log]))

(use-fixtures :once fix/use-test-environment fix/setup-schema)

(deftest core-test
  (testing "Initialization"
    (users/add :testuser-1)
    (users/add :testuser-2) 
    
    (client/send-text :testuser-1 "Hello, bot!")
    (->> (users/wait-for-new-message :testuser-1)
         (client/check-text "123 testuser-1 HELLO, BOT!"))))
