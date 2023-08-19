(ns magic-tray-bot.schema.category)

(def schema [{:db/ident :category/name
              :db/unique :db.unique/identity
              :db/valueType :db.type/string
              :db/cardinality :db.cardinality/one
              :db/doc "Category's name"}

             {:db/ident :category/id ; TODO: Really need it?
              :db/unique :db.unique/identity
              :db/valueType :db.type/keyword
              :db/cardinality :db.cardinality/one
              :db/doc "Category's id"}])
