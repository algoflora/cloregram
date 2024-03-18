(ns cloregram.main-test
  (:require [clojure.test :refer :all]
            [cloregram.handler]
            [cloregram.test-handlers]
            [cloregram.api :as api]
            [cloregram.users :as users]
            [cloregram.test.infrastructure.users :as u]
            [cloregram.test.infrastructure.client :as c]
            [cloregram.test.infrastructure.inspector :as i]
            [nano-id.core :refer [nano-id]]))

(defn main-test
  []
  (with-redefs [cloregram.handler/common cloregram.test-handlers/core]
    (testing "Core"
      (u/add :testuser-1)
      (u/add :testuser-2)
    
      (c/send-text :testuser-1 "Hello, bot!")
      (let [msg (u/main-message :testuser-1)]
        (is (thrown-with-msg? Exception #"^No expected button in Message!$" (c/click-btn msg :testuser-1 "!")))
        (is (thrown-with-msg? Exception #"^No expected button in Message!$" (c/click-btn msg :testuser-1 10 20)))
        (is (thrown-with-msg? Exception #"^Wrong User interacting with Message!$" (c/click-btn msg :testuser-2 "+"))))
    
      (-> (u/main-message :testuser-1)
          (i/check-text "testuser-1 HELLO, BOT!")
          (i/check-btns [["+" "-"]])
          (c/click-btn :testuser-1 "+"))
      (-> (u/main-message :testuser-1)
          (i/check-text "Incremented: 1")
          (i/check-btns [["+" "-"]["Temp"]])
          (c/click-btn :testuser-1 "+"))
      (-> (u/main-message :testuser-1)
          (i/check-text "Incremented: 2")
          (i/check-btns [["+" "-"]["Temp"]])
          (c/click-btn :testuser-1 "+"))
      (-> (u/main-message :testuser-1)
          (i/check-text "Incremented: 3")
          (i/check-btns [["+" "-"]["Temp"]])
          (c/click-btn :testuser-1 1 2))
      (-> (u/main-message :testuser-1)
          (i/check-text "Decremented: 2")
          (i/check-btns [["+" "-"]])
          (c/click-btn :testuser-1 1 1))
      (-> (u/main-message :testuser-1)
          (i/check-text "Incremented: 3")
          (i/check-btns [["+" "-"]["Temp"]])
          (c/click-btn :testuser-1 2 1))
      (-> (u/last-temp-message :testuser-1)
          (i/check-text "Temp message")
          (i/check-btns [["New text"]["✖️"]]))

      (is (= 1 (u/count-temp-messages :testuser-1)))

      (-> (u/last-temp-message :testuser-1)
          (c/click-btn :testuser-1 2 1))

      (is (= 0 (u/count-temp-messages :testuser-1)))

      (-> (u/main-message :testuser-1)
          (i/check-text "Incremented: 3")
          (i/check-btns [["+" "-"]["Temp"]])
          (c/click-btn :testuser-1 2 1))
      (-> (u/last-temp-message :testuser-1)
          (i/check-text "Temp message")
          (i/check-btns [["New text"]["✖️"]]))
      
      (is (= 1 (u/count-temp-messages :testuser-1)))

      (-> (u/last-temp-message :testuser-1)
          (c/click-btn :testuser-1 "New text"))

      (-> (u/last-temp-message :testuser-1)
          (i/check-text "New temp message")
          (i/check-btns [["New text 2"]["✖️"]]))

      (is (= 1 (u/count-temp-messages :testuser-1)))

      (-> (u/last-temp-message :testuser-1)
          (c/click-btn :testuser-1 "New text 2"))

      (-> (u/last-temp-message :testuser-1)
          (i/check-text "New temp message 2")
          (i/check-btns [["New text 2"]["✖️"]]))

      (is (= 1 (u/count-temp-messages :testuser-1)))

      (-> (u/last-temp-message :testuser-1)
          (c/click-btn :testuser-1 "✖️"))

      (is (= 0 (u/count-temp-messages :testuser-1))))

    (testing "Documents"
      (let [path    "/tmp/ss-bot-test-file.txt"
            content (nano-id 512)
            user (u/get-user-by-uid :testuser-1)]
        (spit path content)
        (api/send-document (users/get-by-username (name :testuser-1)) path "Test Caption" [])

        (is (= 1 (u/count-temp-messages :testuser-1)))
        (-> (u/last-temp-message :testuser-1)
            (i/check-document "Test Caption" (.getBytes content))
            (i/check-btns [["✖️"]])
            (c/click-btn :testuser-1 "✖️"))

        (is (= 0 (u/count-temp-messages :testuser-1)))))

    (testing "Invoice"
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
            (c/click-btn :testuser-1 "✖️"))

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
            (c/click-btn :testuser-1 "✖️"))

        (-> (u/last-temp-message :testuser-1)
            (i/check-invoice invoice-data)
            (c/click-btn :testuser-1 "✖️"))
 
        (is (= 0 (u/count-temp-messages :testuser-1)))))))
