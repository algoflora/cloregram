(ns magic-tray-bot.user
  (:require [datomic.api :as d]
            [dialog.logger :as log]
            [magic-tray-bot.db :as db]
            [nano-id.core :refer [nano-id]]))

(defn get-by-username
  "Reads user information by `uname`"
  ([uname] (get-by-username uname (db/get-db)))
  ([uname db]
   (let [user (d/q '[:find ?e
                     :in $ ?uname
                     :where [?e :user/username ?uname]]
                   db uname)]
     (log/debug "User lookup in database:" user)
     user)))

(defn compare-info
  "Compares `in` user data with existing user `uinfo`"
  [in uinfo]
  (log/debug (str "Compare:\t" in "\t" uinfo))
  (and (= (:id in) (:user/chat-id uinfo))
       (= (:first_name in) (:user/first-name uinfo))
       (= (:last_name in) (:user/last-name uinfo))
       (= (:language_code in) (:user/language-code uinfo))))

(defn create!
  "Creates new user or updates existent one from `udata`"
  [udata]
  (d/transact db/conn [{:user/username (:username udata)
                        :user/chat-id (:id udata)
                        :user/first-name (:first_name udata)
                        :user/last-name (:last_name udata)
                        :user/language-code (:language_code udata)
                        ;; :user.flow/instance nil
                        ;; :user.flow/point nil
                        ;; :user.flow/vars []
                        }]))

(defn update-info!
  "Updates info of user with given `username` with `udata`"
  [udata]
  (d/transact db/conn [{:user/username (:username udata)
                        :user/chat-id (:id udata)
                        :user/first-name (:first_name udata)
                        :user/last-name (:last_name udata)
                        :user/language-code (:language_code udata)}]))
