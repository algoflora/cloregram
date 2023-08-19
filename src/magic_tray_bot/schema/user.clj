(ns magic-tray-bot.schema.user)

(def schema [{:db/ident :user.info/username
              :db/unique :db.unique/identity
              :db/valueType :db.type/string
              :db/cardinality :db.cardinality/one
              :db/doc "User's Telegram username"}

             {:db/ident :user.info/chat-id
              :db/unique :db.unique/identity
              :db/valueType :db.type/long
              :db/cardinality :db.cardinality/one
              :db/doc "User's chat ID"}

             {:db/ident :user.info/first-name
              :db/valueType :db.type/string
              :db/cardinality :db.cardinality/one
              :db/doc "User's first name in Telegram profile"}

             {:db/ident :user.info/last-name
              :db/valueType :db.type/string
              :db/cardinality :db.cardinality/one
              :db/doc "User's last name in Telegram profile"}

             {:db/ident :user.info/language-code
              :db/valueType :db.type/string
              :db/cardinality :db.cardinality/one
              :db/doc "User's language code in Telegram profile"}

             {:db/ident :user.flow/point
              :db/valueType :db.type/keyword
              :db/cardinality :db.cardinality/one
              :db/doc "User's current flow point"}

             {:db/ident :user.flow/project
              :db/valueType :db.type/ref
              :db/cardinality :db.cardinality/one
              :db/doc "User's current flow instance of some blueprint"}

             {:db/ident :user.flow/vars
              :db/valueType :db.type/tuple
              :db/tupleTypes [:db.type/keyword :db.type/string]
              :db/cardinality :db.cardinality/many
              :db/doc "User's current flow `variable`s in format [:keyword JSON-:string]"}

             {:db/ident :user/validate
              :db.entity/attrs [:user.info/username :user.info/chat-id]}])
