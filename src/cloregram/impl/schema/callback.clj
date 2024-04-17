(ns ^:no-doc cloregram.impl.schema.callback)

(def callback 
  {:callback/uuid {:db/valueType :db.type/uuid
                   :db/cardinality :db.cardinality/one
                   :db/unique :db.unique/identity
                   :db/doc "UUID of Callback"}

   :callback/function {:db/valueType :db.type/symbol
                       :db/cardinality :db.cardinality/one
                       :db/doc "Qualified symbol of function of Callback"}

   :callback/arguments {:db/cardinality :db.cardinality/one
                        :db/doc "EDN-serialized arguments of Callback"}
   
   :callback/user {:db/valueType :db.type/ref
                   :db/cardinality :db.cardinality/one
                   :db/doc "The User for whom this Callbak is intended"}

   :callback/message-id {:db/valueType :db.type/long
                         :db/cardinality :db.cardinality/one
                         :db/doc "ID of Message this Callback is associated with"}})
