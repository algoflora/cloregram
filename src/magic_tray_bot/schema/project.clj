(ns magic-tray-bot.schema.project)

(def schema [{:db/ident :project/name
              :db/unique :db.unique/identity
              :db/valueType :db.type/string
              :db/cardinality :db.cardinality/one
              :db/doc "Project's name"}

             {:db/ident :project/owner
              :db/valueType :db.type/ref
              :db/cardinality :db.cardinality/one
              :db/doc "Project's owner `User`"}

             {:db/ident :project/validate
              :db.entity/attrs [:project/name :project/owner]}])
