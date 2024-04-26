(ns cloregram.handler
  (:require [cloregram.impl.handler :as impl]
            [cloregram.api :refer [send-message]]))

(defn delete-this-message

  "Handler to delete message. Deletes the message with ID `mid` of `user`. Cleanups callbacks"

  {:pre [(number? *from-message-id*)
         (map? *current-user*)]}
  
  [_]
  (impl/delete-message {:mid *from-message-id* :user *current-user*}))

(defn common

  "Core handler of system. Must be overriden in project."
  
  [{:keys [user]}]
  (send-message user "Hello from Cloregram Framework!" []))

(defn payment

  "Payments handler. Must be overriden in project if payments processing is necessary."
  
  [{:keys [user payment]}]
  (send-message user (str "Successful payment with payload " (:invoice_payload payment)) [] :temp))
