(ns magic-tray-bot.test.infrastructure.logging
  (:require [clojure.string :as str]))

(defn add-test-prefix
  [_ event]
  (if (or (= "magic-tray-bot.core-test" (:logger event))
          (str/starts-with? (:logger event) "magic-tray-bot.test"))
    (update event :message #(str "[TEST INFRA] " %))
    event))
