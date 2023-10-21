(ns magic-tray-bot.system.state)

(defonce system (atom nil))

(defn bot [] (:bot/instance @system))
