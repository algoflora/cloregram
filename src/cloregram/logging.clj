(ns cloregram.logging
  (:require [cloregram.utils :as utl]
            [clojure.stacktrace :refer [print-stack-trace]]))

(defn transform-json
  [_ event]
  (when true #_(not (.contains (:message event) "message is not modified"))
    (cond-> event
      true (dissoc :line)
      true (merge (utl/get-project-info))
      (> (count (:message event)) 20000) (update event :message #(subs % 0 20000))
      (contains? event :error) (update :message
                                       #(str %
                                             "\n\n"
                                             (with-out-str (print-stack-trace (:error event))))))))
