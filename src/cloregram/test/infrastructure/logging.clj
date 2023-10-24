(ns cloregram.test.infrastructure.logging
  (:require [clojure.string :as str]))

(defn add-test-prefix
  [_ event]
  (if (or (= "cloregram.core-test" (:logger event))
          (str/starts-with? (:logger event) "cloregram.test"))
    (update event :message #(str "[TEST INFRA] " %))))
