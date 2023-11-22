(ns cloregram.fixtures
  (:require [cloregram.handler]
            [cloregram.test-handlers]))

(defn set-test-common-handler
  [body]
  (with-redefs [cloregram.handler/common cloregram.test-handlers/common] (body)))
