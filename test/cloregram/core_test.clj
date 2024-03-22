(ns cloregram.core-test
  (:require [clojure.test :refer :all]
            [cloregram.main-test :refer [main-test]]
            [cloregram.media-test :refer [media-test]]
            [cloregram.test.fixtures :as fix]
            [cloregram.handler]
            [cloregram.test-handlers]
            [cloregram.api :as api]
            [cloregram.users :as users]
            [cloregram.test.infrastructure.users :as u]
            [cloregram.test.infrastructure.client :as c]
            [cloregram.test.infrastructure.inspector :as i]
            [nano-id.core :refer [nano-id]]))

(use-fixtures :once
  fix/use-test-environment)

(deftest integration-test
  (u/add :testuser-1)
  (u/add :testuser-2)

  (testing "Integration Test"
    (main-test)
    #_(media-test)))
