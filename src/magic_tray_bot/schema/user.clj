(ns magic-tray-bot.schema.user)

(def schema [{:db/ident :user/username
              :db/valueType :db.type/string
              :db/cardinality :db.cardinality/one
              :db/unique :db.unique/identity
              :db/doc "User's Telegram username"}

             {:db/ident :user/chat-id
              :db/valueType :db.type/long
              :db/cardinality :db.cardinality/one
              :db/unique :db.unique/identity
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

             {:db/ident :user/point
              :db/valueType :db.type/keyword
              :db/cardinality :db.cardinality/one
              :db/doc "User's current flow point"}

             {:db/ident :user/place
              :db/valueType :db.type/ref
              :db/cardinality :db.cardinality/one
              :db/doc "User's current flow instance of some blueprint"}

             {:db/ident :user/vars
              :db/valueType :db.type/tuple
              :db/tupleTypes [:db.type/keyword :db.type/string]
              :db/cardinality :db.cardinality/many
              :db/doc "User's current flow `variable`s in format [:keyword JSON-:string]"}

             {:db/ident :user/validate
              :db.entity/attrs [:user/username :user/chat-id :user/point :user/vars]
              :db.entity/preds 'magic-tray-bot.schema.predicats.user.point-valid?}])
