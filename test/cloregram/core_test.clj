(ns cloregram.core-test
  (:require [clojure.test :refer [is use-fixtures deftest testing]]
            [cloregram.test.fixtures :as fix]
            [cloregram.test.infrastructure.users :as u]
            [cloregram.test.infrastructure.client :as c]
            [cloregram.test.infrastructure.inspector :as i]
            [dialog.logger :as log]))

(use-fixtures :once fix/use-test-environment fix/setup-schema)

(deftest core-test
  (testing "Initialization"
    (u/add :testuser-1)
    (u/add :testuser-2) 
    
    (c/send-text :testuser-1 "Hello, bot!")
    (-> (u/wait-main-message :testuser-1)
        (i/check-text "testuser-1 HELLO, BOT!")
        (i/check-btns [["+" "-"]])
        (c/press-btn :testuser-1 "+"))
    (-> (u/wait-main-message :testuser-1)
        (i/check-text "Incremented: 1")
        (i/check-btns [["+" "-"]["Temp"]])
        (c/press-btn :testuser-1 "+"))
    (-> (u/wait-main-message :testuser-1)
        (i/check-text "Incremented: 2")
        (i/check-btns [["+" "-"]["Temp"]])
        (c/press-btn :testuser-1 "+"))
    (-> (u/wait-main-message :testuser-1)
        (i/check-text "Incremented: 3")
        (i/check-btns [["+" "-"]["Temp"]])
        (c/press-btn :testuser-1 1 2))
    (-> (u/wait-main-message :testuser-1)
        (i/check-text "Decremented: 2")
        (i/check-btns [["+" "-"]])
        (c/press-btn :testuser-1 1 1))
    (-> (u/wait-main-message :testuser-1)
        (i/check-text "Incremented: 3")
        (i/check-btns [["+" "-"]["Temp"]])
        (c/press-btn :testuser-1 2 1))
    (-> (u/wait-temp-message :testuser-1)
        (i/check-text "Temp message")
        (i/check-btns [["✖️"]]))
    (is (= 1 (u/count-temp-messages :testuser-1)))
    (-> (u/get-last-temp-message :testuser-1)
        (c/press-btn :testuser-1 1 1))
    (Thread/sleep 100)
    (is (= 0 (u/count-temp-messages :testuser-1)))))
