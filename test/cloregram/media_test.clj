(ns cloregram.media-test
  (:require [clojure.test :refer :all]
            [cloregram.handler]
            [cloregram.test-handlers]
            [cloregram.test.infrastructure.client :as c]
            [cloregram.test.infrastructure.users :as u]
            [cloregram.test.infrastructure.inspector :as i]))

(defn media-test
  []
  (with-redefs [cloregram.handler/common cloregram.test-handlers/photo-handler]
    (testing "Photo"
      (c/send-photo :testuser-1 "Algoflora Logo" "weedbreed.png")
      (-> (u/last-temp-message :testuser-1)
          (i/check-photo "Flipped!" "weedbreed-flipped.png"))

      (Thread/sleep 1000)
      (is (= 1 1)))))
