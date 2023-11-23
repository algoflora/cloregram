(ns cloregram.schema.callback)

(def callback 
  [{:db/ident :callback/uuid
    :db/valueType :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "UUID of Callback"}

   {:db/ident :callback/function
    :db/valueType :db.type/symbol
    :db/cardinality :db.cardinality/one
    :db/doc "Qualified symbol of function of Callback"}

   {:db/ident :callback/args
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "EDN-serialized arguments of Callback"}

   {:db/ident :callback/user
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The User for whom this Callbak is intended"}

   {:db/ident :callback/validate
    :db.entity/attrs [:callback/uuid :callback/function :callback/user]}])
