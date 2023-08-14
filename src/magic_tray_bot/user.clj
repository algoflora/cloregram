(ns magic-tray-bot.user
  (:require [datomic.api :as d]
            [dialog.logger :as log]
            [magic-tray-bot.db :as db]
            [nano-id.core :refer [nano-id]]))

(defn get-by-username
  "Reads user information by `uname`"
  ([uname] (get-by-username uname (db/get-db)))
  ([uname db]
   (let [user (ffirst (d/q '[:find (pull ?e [*])
                             :in $ ?uname
                             :where [?e :user.info/username ?uname]]
                           db uname))]
     (log/debug "User lookup in database:" user)
     user)))

(defn compare-info
  "Compares `in` user data with existing user `uinfo`"
  [in uinfo]
  (log/debug (str "Compare:\t" in "\t" uinfo))
  (and (= (:id in) (:user.info/chat-id uinfo))
       (= (:first_name in) (:user.info/first-name uinfo))
       (= (:last_name in) (:user.info/last-name uinfo))
       (= (:language_code in) (:user.info/language-code uinfo))))

(defn create!
  "Creates new user or updates existent one from `udata`"
  [udata]
  (d/transact db/conn [{:user.info/username (:username udata)
                        :user.info/chat-id (:id udata)
                        :user.info/first-name (:first_name udata)
                        :user.info/last-name (:last_name udata)
                        :user.info/language-code (:language_code udata)
                        ;; :user.flow/instance nil
                        ;; :user.flow/point nil
                        ;; :user.flow/vars []
                        }]))

(defn update-info!
  "Updates info of user with given `username` with `udata`"
  [udata]
  (d/transact db/conn [{:user.info/username (:username udata)
                        :user.info/chat-id (:id udata)
                        :user.info/first-name (:first_name udata)
                        :user.info/last-name (:last_name udata)
                        :user.info/language-code (:language_code udata)}]))
