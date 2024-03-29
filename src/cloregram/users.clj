(ns cloregram.users
  (:require [taoensso.timbre :as log]
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
      (do (log/debug "Loaded User" {:user user})
          user) ; TODO: Check info and update if needed
      (do (d/transact (db/conn) [(->> {:user/id (:id udata)
                                       :user/username (:username udata)
                                       :user/first-name (:first_name udata)
                                       :user/last-name (:last_name udata)
                                       :user/language-code (:language_code udata)
                                       :user/handler [(symbol (str (:name (utl/get-project-info)) ".handler/common")) nil]}
                                      (filter second) ; 'false' values will be removed!
                                      (into {}))])
          (log/info "Created User" {:user-data udata})
          (get-or-create udata)))))

(defn set-msg-id
  [user msg-id]
  (log/debug "Setting main Message ID for User" {:msg-id msg-id :user user})
  (d/transact (db/conn) [{:user/id (:user/id user)
                          :user/msg-id msg-id}]))

(defn set-handler
  [user handler args]
  (log/debug "Setting Handler for User" {:handler-function handler :handler-arguments args :user user})
  (d/transact (db/conn) [{:user/id (:user/id user)
                          :user/handler [handler (prn-str args)]}]))
