(ns cloregram.users
  (:require [cloregram.impl.users :as impl]))

(defn load-by-username

  "Loads User structure by provided `username`. Useful for testing purposes."
  
  [username]
  (impl/load-by-username username))

(defn set-handler

  "Set qualified symbol `handler` with `args` to `user`. This handler will be called instead of common one in case of text or media input from user."

  {:changed "0.10.1"}

  [user handler args]
  (impl/set-handler user handler args))
