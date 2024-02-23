(ns cloregram.system.state)

(defonce system (atom nil))

(defn bot
  "Returns bot instance"
  [] (:bot/instance @system))

(defn config
  "Returns value (or nil) from project config nested map, using chain of `keys`"
  {:changed "0.5.4"}
  [& keys]
  (get-in (:project/config @system) keys))
