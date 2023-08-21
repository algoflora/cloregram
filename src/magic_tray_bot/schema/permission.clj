(ns magic-tray-bot.schema.permission)

(def schema [{:db/ident :permission/type
              :db/valueType :db.type/ref
              :db/cardinality :db.cardinality/one
              :db/doc "Permission's `Type`"}

             {:db/ident :permission/user
              :db/valueType :db.type/ref
              :db/cardinality :db.cardinality/one
              :db/doc "`User` with the Permission"}

             {:db/ident :permission/place
              :db/valueType :db.type/ref
              :db/cardinality :db.cardinality/one
              :db/doc "`Place` of the Permission"}

             {:db/ident :permission/type+user+place
              :db/valueType :db.type/tuple
              :db/tupleAttrs [:permission/type :permission/user :permission/place]
              :db/cardinality :db.cardinality/one
              :db/unique :db.unique/identity}

             {:db/ident :permission/categories
              :db/valueType :db.type/ref
              :db/cardinality :db.cardinality/many
              :db/doc "`Categories` covered by the Permission"}

             {:db/ident :permission/validate
              :db.entity/attrs [:permission/type :permission/user :permission/place]}])

