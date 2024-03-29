(ns cloregram.core-test
  (:require [clojure.test :refer [is use-fixtures deftest testing]]
            [cloregram.test.fixtures :as fix]
            [cloregram.fixtures]
            [cloregram.api :as api]
            [cloregram.users :as users]
            [cloregram.test.infrastructure.users :as u]
            [cloregram.test.infrastructure.client :as c]
            [cloregram.test.infrastructure.inspector :as i]
            [nano-id.core :refer [nano-id]]))

(use-fixtures :once
  fix/use-test-environment
  cloregram.fixtures/set-test-common-handler)

(deftest core-test
  (testing "Initialization"
    (u/add :testuser-1)
    (u/add :testuser-2) 
    
    (c/send-text :testuser-1 "Hello, bot!")
    (let [msg (u/main-message :testuser-1)]
      (is (thrown-with-msg? Exception #"^No expected button in Message!$" (c/press-btn msg :testuser-1 "!")))
      (is (thrown-with-msg? Exception #"^No expected button in Message!$" (c/press-btn msg :testuser-1 10 20)))
      (is (thrown-with-msg? Exception #"^Wrong User interacting with Message!$" (c/press-btn msg :testuser-2 "+"))))
    
    (-> (u/main-message :testuser-1)
        (i/check-text "testuser-1 HELLO, BOT!")
        (i/check-btns [["+" "-"]])
        (c/press-btn :testuser-1 "+"))
    (-> (u/main-message :testuser-1)
        (i/check-text "Incremented: 1")
        (i/check-btns [["+" "-"]["Temp"]])
        (c/press-btn :testuser-1 "+"))
    (-> (u/main-message :testuser-1)
        (i/check-text "Incremented: 2")
        (i/check-btns [["+" "-"]["Temp"]])
        (c/press-btn :testuser-1 "+"))
    (-> (u/main-message :testuser-1)
        (i/check-text "Incremented: 3")
        (i/check-btns [["+" "-"]["Temp"]])
        (c/press-btn :testuser-1 1 2))
    (-> (u/main-message :testuser-1)
        (i/check-text "Decremented: 2")
        (i/check-btns [["+" "-"]])
        (c/press-btn :testuser-1 1 1))
    (-> (u/main-message :testuser-1)
        (i/check-text "Incremented: 3")
        (i/check-btns [["+" "-"]["Temp"]])
        (c/press-btn :testuser-1 2 1))
    (-> (u/last-temp-message :testuser-1)
        (i/check-text "Temp message")
        (i/check-btns [["✖️"]]))
    (is (= 1 (u/count-temp-messages :testuser-1)))
    (-> (u/last-temp-message :testuser-1)
        (c/press-btn :testuser-1 1 1))

    (is (= 0 (u/count-temp-messages :testuser-1)))

    ;; Documents

    (let [path    "/tmp/ss-bot-test-file.txt"
          content (nano-id 512)
          user (u/get-user-by-uid :testuser-1)]
      (spit path content)
      (api/send-document (users/get-by-username (name :testuser-1)) path "Test Caption" [])

      (is (= 1 (u/count-temp-messages :testuser-1)))
      (-> (u/last-temp-message :testuser-1)
          (i/check-document "Test Caption" (.getBytes content))
          (i/check-btns [["✖️"]])
          (c/press-btn :testuser-1 "✖️"))

      (is (= 0 (u/count-temp-messages :testuser-1))))

    ;; Invoice

    (let [invoice-data {:title "TITLE"
                        :description "DESCRIPTION"
                        :payload {:a 1 :b {:c 2}}
                        :provider_token "PROVIDER_TOKEN"
                        :currency "CUR"
                        :prices [{:label "PRICE-1" :amount 1000}
                                 {:label "PRICE-2" :amount 4200}]}]
      (api/send-invoice (users/get-by-username (name :testuser-1))
                        invoice-data
                        "PAY TEST"
                        [[["BUTTON" 'cloregram.handler/common]]])

      (-> (u/last-temp-message :testuser-1)
          (i/check-invoice invoice-data)
          (i/check-btns [["PAY TEST"]["BUTTON"]["✖️"]])
          (c/press-btn :testuser-1 "✖️"))

      (is (= 0 (u/count-temp-messages :testuser-1)))

      (api/send-invoice (users/get-by-username (name :testuser-1))
                        invoice-data
                        "PAY TEST"
                        [[["BUTTON" 'cloregram.handler/common]]])

      (-> (u/last-temp-message :testuser-1)
          (i/check-invoice invoice-data)
          (i/check-btns [["PAY TEST"]["BUTTON"]["✖️"]])
          (c/pay-invoice :testuser-1))

      (-> (u/last-temp-message :testuser-1)
          (i/check-text "Successful payment with payload {:a 1, :b {:c 2}}")
          (c/press-btn :testuser-1 "✖️"))

      (-> (u/last-temp-message :testuser-1)
          (i/check-invoice invoice-data)
          (c/press-btn :testuser-1 "✖️"))
 
      (is (= 0 (u/count-temp-messages :testuser-1))))))
