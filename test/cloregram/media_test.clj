(ns cloregram.media-test
  (:require [clojure.test :refer :all]
            [cloregram.handler]
            [cloregram.test-handlers]
            [cloregram.validation.client :as c]
            [cloregram.validation.users :as u]
            [cloregram.validation.inspector :as i]))

(defn media-test
  []
  (with-redefs [cloregram.handler/common cloregram.test-handlers/photo-handler]
    (testing "Photo"
      (c/send-photo :testuser-1 "Algoflora Logo" "algoflora.png")
      (-> (u/last-temp-message :testuser-1)
          (i/check-photo "Flipped!" "algoflora-flipped.png"))

      (Thread/sleep 1000)
      (is (= 1 1)))))
