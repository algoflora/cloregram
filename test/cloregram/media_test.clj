(ns cloregram.media-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [cloregram.handlers]
            [cloregram.test-handlers]
            [cloregram.api :as api]
            [cloregram.users :as users]
            [cloregram.validation.client :as c]
            [cloregram.validation.users :as u]
            [cloregram.validation.inspector :as i]))

(defn media-test
  []
  (with-redefs [cloregram.handlers/main cloregram.test-handlers/photo-handler]
    (testing "Photo"
      (c/send-photo :testuser-1 "Algoflora Logo" "algoflora.png")
      (let [msg (u/last-temp-message :testuser-1)
            user (users/load-by-username (name :testuser-1))]
        (i/check-photo msg "Flipped!" "algoflora-flipped.png")
        (is (= 1 (u/count-temp-messages :testuser-1)))

        (u/set-waiting-for-response :testuser-1 true)
        (api/send-photo user (-> "algoflora.png" io/resource io/file) "1" [] (:message_id msg))

        (let [msg-1 (u/last-temp-message :testuser-1)]
          (is (= 1 (u/count-temp-messages :testuser-1)))
          (i/check-photo msg-1 "1" "algoflora.png")

          (u/set-waiting-for-response :testuser-1 true)
          (api/send-photo user (-> "algoflora-flipped.png" io/resource io/file) "2" [] (:message_id msg))

          (let [msg-2 (u/last-temp-message :testuser-1)]
            (is (= 1 (u/count-temp-messages :testuser-1)))
            (i/check-photo msg-2 "2" "algoflora-flipped.png")

            (u/set-waiting-for-response :testuser-1 true)
            (api/send-photo user (-> "algoflora.png" io/resource io/file) "3" [] -1)

            (let [msg-3 (u/last-temp-message :testuser-1)]
              (is (= 2 (u/count-temp-messages :testuser-1)))
              (i/check-photo msg-3 "3" "algoflora.png")
              (c/click-btn msg-3 :testuser-1 1 1)

              (let [msg-2? (u/last-temp-message :testuser-1)]
                (is (= 1 (u/count-temp-messages :testuser-1)))
                (i/check-photo msg-2? "2" "algoflora-flipped.png")
                (c/click-btn msg-2? :testuser-1 1 1)

                (is (= 0 (u/count-temp-messages :testuser-1))))))))

      (Thread/sleep 1000)
      (is (= 1 1)))))
