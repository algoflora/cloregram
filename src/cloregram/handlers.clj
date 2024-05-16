(ns cloregram.handlers
  (:require [cloregram.api :refer [send-message delete-message]]
            [cloregram.dynamic :refer :all]))

(defn delete-this-message

  "Handler to delete message. Deletes the message with was called from. Cleanups callbacks"

  {:pre [(number? *from-message-id*)
         (map? *current-user*)]}
  
  [_]
  (delete-message *current-user* *from-message-id*))

(defn main

  "Core handler of system. Must be overriden in project."
  
  [_]
  (send-message *current-user*
                "Hello from Cloregram Framework!" []))

(defn payment

  "Payments handler. Must be overriden in project if payments processing is necessary."
  
  [{:keys [payment]}]
  (send-message *current-user*
                (str "Successful payment with payload " (:invoice_payload payment)) [] :temp))
