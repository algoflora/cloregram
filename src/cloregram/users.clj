(ns cloregram.users
  (:require [dialog.logger :as log]
            [cloregram.db :as db]
            [datomic.api :as d]
            [cloregram.utils :as utl]))

(defn get-list
  []
  (d/q '[:find (pull ?u [*])
         :where [?u :user/username]] (db/db)))

(defn get-by-username
  [uname]
  (d/pull (db/db) '[*] [:user/username uname]))

(defn get-or-create
  [udata]
  (let [user (d/pull (db/db) '[*] [:user/id (:id udata)])]
    (if (some? (:db/id user))
      (do (log/debug "Loaded User:" user)
          user) ; TODO: Check info and update if needed
      (do (d/transact (db/conn) [(->> {:user/id (:id udata)
                                       :user/username (:username udata)
                                       :user/first-name (:first_name udata)
                                       :user/last-name (:last_name udata)
                                       :user/language-code (:language_code udata)
                                       :user/handler [(symbol (str (:name (utl/get-project-info)) ".handler/common")) nil]}
                                      (filter second) ; 'false' values will be removed!
                                      (into {}))])
          (log/info "Created User by data:" udata)
          (get-or-create udata)))))

(defn set-msg-id
  [user msg-id]
  (log/debugf "Setting msg-id=%d for User %s" msg-id (utl/username user))
  (d/transact (db/conn) [{:user/id (:user/id user)
                          :user/msg-id msg-id}]))

(defn set-handler
  [user handler args]
  (log/debugf "Setting handler %s with arguments %s for User %s" handler args user)
  (d/transact (db/conn) [{:user/id (:user/id user)
                          :user/handler [handler args]}]))
