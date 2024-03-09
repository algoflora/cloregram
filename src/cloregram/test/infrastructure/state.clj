(ns cloregram.test.infrastructure.state)

(defonce webhook-address (atom nil))

(defonce webhook-token (atom ""))

(defonce update-id (atom 0))

(defonce users (atom {}))

(defonce checkout-queries (atom {}))

(defonce files (atom {}))
