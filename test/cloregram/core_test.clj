(ns cloregram.core-test
  (:require [clojure.test :refer :all]
            [cloregram.main-test :refer [main-test]]
            [cloregram.media-test :refer [media-test]]
            [cloregram.validation.fixtures :as fix]
            [cloregram.test-handlers]
            [cloregram.validation.users :as u]))

(use-fixtures :once
  fix/use-test-environment)

(deftest integration-test
  (u/add :testuser-1)
  (u/add :testuser-2)

  (testing "Integration Test"
    (main-test)
    (media-test)))
