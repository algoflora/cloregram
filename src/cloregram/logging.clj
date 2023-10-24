(ns cloregram.logging
  (:require [cloregram.utils :as utl]))

(defn transform-json
  [_ event]
  (-> event
      (dissoc :line)
      (merge (utl/get-project-info))))
