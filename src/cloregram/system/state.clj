(ns cloregram.system.state)

(defonce system (atom nil))

(defn bot [] (:bot/instance @system))

(defn config [] (:project/config @system))
