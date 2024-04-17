(ns ^:no-doc cloregram.impl.validation.state)

(defonce webhook-address (atom nil))

(defonce webhook-token (atom ""))

(defonce update-id (atom 0))

(defonce v-users (atom {}))

(defonce checkout-queries (atom {}))

(defonce files (atom {}))
