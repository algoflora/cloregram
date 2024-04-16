(ns cloregram.impl.state)

(defonce system (atom nil))

(defn bot
  "Returns bot instance"
  [] (:bot/instance @system))
