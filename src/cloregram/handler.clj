(ns cloregram.handler
  (:require [cloregram.impl.handler :as impl]
            [cloregram.api :refer [send-message]]))

(defn delete-message

  "Handler to delte message. Deletes the message with ID `mid` of `user`. Cleanups callbacks"
  
  [{:keys [mid user] :as params}]
  (impl/delete-message params))

(defn common

  "Core handler of system. Must be overriden in project."
  
  [{:keys [user]}]
  (send-message user "Hello from Cloregram Framework!" []))

(defn payment

  "Payments handler. Must be overriden in project if payments processing is necessary."
  
  [{:keys [user payment]}]
  (send-message user (str "Successful payment with payload " (:invoice_payload payment)) [] :temp))
