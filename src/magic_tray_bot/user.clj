(ns magic-tray-bot.user
  (:require [dialog.logger :as log]
            [xtdb.api :as xt]
            [magic-tray-bot.db :refer [xtdb-node]]
            [nano-id.core :refer [nano-id]]))

(defn get-by-username
  "Reads user information by username"
  [uname]
  (let [users (xt/q (xt/db xtdb-node) '{:find [(pull u [*])]
                                        :in [uname]
                                        :where [[u :user/username uname]]}
                    uname)]
    (first (first users))))

(defn compare-info
  "Compares `in` user data with existing user `uinfo`"
  [in uinfo]
  (log/debug (str "Compare:\t" in "\t" uinfo))
  (and (= (:id in) (:user/chat-id uinfo))
       (= (:first_name in) (:user/first-name uinfo))
       (= (:last_name in) (:user/last-name uinfo))
       (= (:language_code in) (:user/language-code uinfo))))

(defn create!
  "Creates new user from `udata`"
  [udata]
  (let [user-id (nano-id)]
    (xt/submit-tx xtdb-node [[::xt/put
                              {:xt/id user-id
                               :user/username (:username udata)
                               :user/chat-id (:id udata)
                               :user/first-name (:first_name udata)
                               :user/last-name (:last_name udata)
                               :user/language-code (:language_code udata)
                               :user/instance nil
                               :user/point :main-menu
                               :user/variables {}}]])
    (get-by-username (:username udata))))

(defn update-info!
  "Updates info of user with given `username` with `user-data`"
  [uname user-data]
  (throw (Exception. "NOT IMPLEMENTED!")))
