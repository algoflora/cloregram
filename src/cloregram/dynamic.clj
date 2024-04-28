(ns cloregram.dynamic)

(def ^{:dynamic true
       :doc "User from whom the current Update came"
       :added "0.11.0"}
  *current-user* nil)

(def ^{:dynamic true
       :doc "ID of Message from which the current callback query came"
       :added "0.11.0"}
  *from-message-id* nil)
