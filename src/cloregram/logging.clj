(ns cloregram.logging
  (:require [cloregram.utils :as utl]))

(defn transform-json
  [_ event]
  (when (not (.contains (:message event) "message is not modified"))
    (cond-> event
      true (dissoc :line)
      true (merge (utl/get-project-info))
      (contains? event :error) (update :message #(str % "\n\n" (:error event))))))
