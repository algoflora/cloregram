(ns cloregram.core-test
  (:require [clojure.test :refer :all]
            [cloregram.test.fixtures :as fix]
            [cloregram.test.infrastructure.users :as u]
            [cloregram.test.infrastructure.client :as c]
            [dialog.logger :as log]))

(use-fixtures :once fix/use-test-environment fix/setup-schema)

(deftest core-test
  (testing "Initialization"
    (u/add :testuser-1)
    (u/add :testuser-2) 
    
    (c/send-text :testuser-1 "Hello, bot!")
    (-> (u/wait-for-new-message :testuser-1)
        (c/check-text "testuser-1 HELLO, BOT!")
        (c/check-btns "+" "-")
        (c/press-btn :testuser-1 "+"))
    (-> (u/wait-for-new-message :testuser-1)
        (c/check-text "Incremented: 1")
        (c/check-btns "+" "-")
        (c/press-btn :testuser-1 "+"))
    (-> (u/wait-for-new-message :testuser-1)
        (c/check-text "Incremented: 2")
        (c/check-btns "+" "-")
        (c/press-btn :testuser-1 "+"))
    (-> (u/wait-for-new-message :testuser-1)
        (c/check-text "Incremented: 3")
        (c/check-btns "+" "-")
        (c/press-btn :testuser-1 "-"))
    (-> (u/wait-for-new-message :testuser-1)
        (c/check-text "Decremented: 2")
        (c/check-btns "+" "-")
        (c/press-btn :testuser-1 "+"))
    (-> (u/wait-for-new-message :testuser-1)
        (c/check-text "Incremented: 3")
        (c/check-btns "+" "-")
        (c/press-btn :testuser-1 "+"))))
