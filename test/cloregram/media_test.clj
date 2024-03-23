(ns cloregram.media-test
  (:require [clojure.test :refer :all]
            [cloregram.handler]
            [cloregram.test-handlers]
            [cloregram.test.infrastructure.client :as c]))

(defn media-test
  []
  (with-redefs [cloregram.handler/common cloregram.test-handlers/photo-handler]
    (testing "Photo"
      (c/send-photo :testuser-1 "Algoflora Logo" "weedbreed.jpg")

      (Thread/sleep 1000)
      (is (= 1 1)))))
