(ns magic-tray-bot.schema.permission-type)

(def schema [{:db/ident :permission.type/name
              :db/valueType :db.type/string
              :db/cardinality :db.cardinality/one
              :db/unique :db.unique/identity
              :db/doc "Permission Type's name"}

             {:db/ident :permission.type/code
              :db/valueType :db.type/keyword
              :db/cardinality :db.cardinality/one
              :db/unique :db.unique/identity
              :db/doc "Permission Type's keyword code"}

             {:db/ident :permission.type/description
              :db/valueType :db.type/string
              :db/cardinality :db.cardinality/one
              :db/doc "Permission Type's description shown to user"}

             {:db/ident :permission.type/categorized
              :db/valueType :db.type/boolean
              :db/cardinality :db.cardinality/one
              :db/doc "Is `Categories` list required for `Permission` of this Permission Type?"}

             {:db/ident :permission.type/module
              :db/valueType :db.type/ref
              :db/cardinality :db.cardinality/one
              :db/doc "`Module` needed to activate Permission Type"}

             {:db/ident :permission.type/validate
              :db.entity/attrs [:permission.type/name
                                :permission.type/code
                                :permission.type/categorized]}])
