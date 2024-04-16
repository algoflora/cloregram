(ns cloregram.impl.schema.user)

(def user
  {:user/username {:db/valueType :db.type/string
                   :db/cardinality :db.cardinality/one
                   :db/unique :db.unique/identity
                   :db/doc "User's Telegram username"}

   :user/id {:db/valueType :db.type/long
             :db/cardinality :db.cardinality/one
             :db/unique :db.unique/identity
             :db/doc "User's ID (and chat_id in private chats)"}
   
   :user/first-name {:db/valueType :db.type/string
                     :db/cardinality :db.cardinality/one
                     :db/doc "User's first name in Telegram profile"}

   :user/last-name {:db/valueType :db.type/string
                    :db/cardinality :db.cardinality/one
                    :db/doc "User's last name in Telegram profile"}

   :user/language-code {:db/valueType :db.type/string
                        :db/cardinality :db.cardinality/one
                        :db/doc "User's language code in Telegram profile"}

   :user/msg-id {:db/valueType :db.type/long
                 :db/cardinality :db.cardinality/one
                 :db/doc "ID of 'main' chat message for this User"}
   
   :user/handler-function {:db/valueType :db.type/symbol
                           :db/cardinality :db.cardinality/one
                           :db/doc "Default handler symbol to use if not a callback query is in incoming update"}

   :user/handler-arguments {:db/cardinality :db.cardinality/one
                            :db/doc "Handler arguments if not a Callback query is in incoming update"}})
