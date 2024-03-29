(ns cloregram.schema.user)

(def user
  [{:db/ident :user/username
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "User's Telegram username"}

   {:db/ident :user/id
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "User's ID (and chat_id in private chats)"}

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

   {:db/ident :user/msg-id
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "ID of 'main' chat message for this User"}
   
   {:db/ident :user/handler
    :db/valueType :db.type/tuple
    :db/tupleTypes [:db.type/symbol :db.type/string]
    :db/cardinality :db.cardinality/one
    :db/doc "Default handler and EDN-serialized args if not a callback query in incoming update"}

   {:db/ident :user/validate
    :db.entity/attrs [:user/id :user/first-name :user/language-code :user/msg-id :user/handler]}])
