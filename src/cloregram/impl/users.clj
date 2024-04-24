(ns ^:no-doc cloregram.impl.users
  (:require [com.brunobonacci.mulog :as μ]
            [cloregram.database :as db]
            [datalevin.core :as d]
            [cloregram.utils :as utl]))

(defn get-list
  []
  (d/q '[:find (pull ?u [*])
         :where [?u :user/username]] (db/db)))

(defn load-by-username
  [username]
  (d/pull (db/db) '[*] [:user/username username]))

(defn- load-by-udata
  [udata]
  (d/pull (db/db) '[*] [:user/id (:id udata)]))

(defn- is-new-udata?
  [udata user]
  (or (not= (:username udata) (:user/username user))
      (not= (:first_name udata) (:user/first-name user))
      (not= (:last_name udata) (:user/last-name user))
      (not= (:language_code udata) (:user/language-code user))))

(defn- renew
  [udata]
  (d/transact! (db/conn) [(->> {:user/id (:id udata)
                                :user/username (:username udata)
                                :user/first-name (:first_name udata)
                                :user/last-name (:last_name udata)
                                :user/language-code (:language_code udata)}
                               (filter #(-> % second some?))
                               (into {}))])
  (let [user (load-by-udata udata)]
    (μ/log ::user-renewed :user-renewed/user user)
    user))


(defn- create
  [udata]
  (d/transact! (db/conn) [(->> {:user/id (:id udata)
                                :user/username (:username udata)
                                :user/first-name (:first_name udata)
                                :user/last-name (:last_name udata)
                                :user/language-code (:language_code udata)
                                :user/handler-function (symbol (str (:name (utl/get-project-info)) ".handler/common"))
                                :user/handler-arguments {}}
                               (filter #(-> % second some?))
                               (into {}))])
  (let [user (load-by-udata udata)]
    (μ/log ::user-created :user-created/user user)
    user))

(defn load-or-create
  [udata]
  (let [user? (load-by-udata udata)
        user  (cond
                (nil? user?)                (create udata)
                (is-new-udata? udata user?) (renew udata)
                :else                       user?)]
    (μ/log ::user-loaded :user-loaded/user user)
    user))

(defn set-msg-id
  [user msg-id]
  (d/transact! (db/conn) [{:user/id (:user/id user)
                           :user/msg-id msg-id}])
  (μ/log ::set-msg-id :set-msg-id/user user :set-msg-id/msg-id msg-id))

(defn set-handler
  [user handler args]
  (d/transact! (db/conn) [{:user/id (:user/id user)
                           :user/handler-function handler
                           :user/handler-arguments (if (nil? args) {} args)}])
  (μ/log ::set-handler :set-handler/user user :set-handler/handler handler :set-handler/arguments args))
