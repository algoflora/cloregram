(ns cloregram.test.infrastructure.state)

(defonce webhook-address (atom nil))

(defonce update-id (atom 0))

(defonce users (atom {}))
