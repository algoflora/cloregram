(ns magic-tray-bot.schema.user)

(def schema [{:db/ident :user/username
              :db/unique :db.unique/identity
              :db/valueType :db.type/string
              :db/cardinality :db.cardinality/one
              :db/doc "User's Telegram username"}

             {:db/ident :user/chat-id
              :db/unique :db.unique/identity
              :db/valueType :db.type/long
              :db/cardinality :db.cardinality/one
              :db/doc "User's chat ID"}

             {:db/ident :user/first-name
              :db/valueType :db.type/string
              :db/cardinality :db.cardinality/one
              :db/doc "User's first name in Telegram profile"}

             {:db/ident :user/last-name
              :db/valueType :db.type/string
              :db/cardinality :db.cardinality/one
              :db/doc "User's last name in Telegram profile"}

             {:db/ident :user/language-code
              :db/valueType :db.type/string
              :db/cardinality :db.cardinality/one
              :db/doc "User's language code in Telegram profile"}

             {:db/ident :user.flow/point
              :db/valueType :db.type/ref
              :db/cardinality :db.cardinality/one
              :db/doc "User's current flow point in some blueprint"}

             {:db/ident :user.flow/instance
              :db/valueType :db.type/ref
              :db/cardinality :db.cardinality/one
              :db/doc "User's current flow instance of some blueprint"}

             {:db/ident :user.flow/vars
              :db/valueType :db.type/tuple
              :db/cardinality :db.cardinality/many
              :db/doc "User's current flow variables in format [<^String name> <value>]"}

             {:db/ident :user/validate
              :db.entity/attrs [:user/username :user/chat-id]}])
