(ns magic-tray-bot.schema.place)

(def schema [{:db/ident :place/name
              :db/valueType :db.type/string
              :db/cardinality :db.cardinality/one
              :db/unique :db.unique/identity
              :db/doc "Project's name"}

             {:db/ident :place/owner
              :db/valueType :db.type/ref
              :db/cardinality :db.cardinality/one
              :db/doc "Project's owner `User`"}

             {:db/ident :place/validate
              :db.entity/attrs [:place/name :place/owner]}])
