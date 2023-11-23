(ns cloregram.api
  (:require [cloregram.system.state :refer [bot]]
            [cloregram.utils :as utl]
            [cloregram.callbacks :as clb]
            [cloregram.users :as u]
            [telegrambot-lib.core :as tbot]
            [dialog.logger :as log]))

(defn- check-opt
  [opt opts]
  (boolean (some #{opt} opts)))

(defn- prepare-options-map
  [opts]
  (let [os #{:temp :markdown}]
    (reduce #(assoc %1 %2 (check-opt %2 opts)) {} os)))

(defmulti ^:private create-key (fn [kdata _] (type kdata)))

(defmethod create-key clojure.lang.PersistentVector
  [kvec user]
  (create-key {:text (first kvec)
               :func (second kvec)
               :args (nth kvec 2)}
              user))

(defmethod create-key clojure.lang.PersistentArrayMap
  [kmap user]
  (cond-> kmap
    (every? #(contains? kmap %) [:func :args])
    (assoc :callback_query (str (clb/create user (:func kmap) (:args kmap))))

    true (dissoc :func)
    true (dissoc :args)))

(defn- prepare-keyboard
  [kbd user optm]
  (let [mapf #(create-key % user)]
    (cond->> kbd
      true (map #(map mapf %))
      (:temp optm) (#(conj % [{:text "✖️"
                               :callback_query (str (java.util.UUID/randomUUID))}])))))

(defn- to-edit?
  [optm user] (and (not (:temp optm)) (some? (:user/msg-id user))))

(defn- prepare-arguments-map
  [argm optm user]
  (cond-> argm
    true (assoc :chat_id (:user/id user))
    (:markdown optm) (assoc :parse_mode "Markdown")
    (to-edit? optm user) (assoc :message_id (:user/msg-id user))))

(defmulti ^:private send-to-chat (fn [& args] (identity (first args))))

(defmethod send-to-chat :message
  [_ user text kbd optm]
  (let [argm (prepare-arguments-map {:text text :reply_markup kbd} optm user)
        func (if (to-edit? optm user)
               tbot/edit-message-text tbot/send-message)
        new-msg (utl/api-wrap func (bot) argm)
        new-msg-id (:message_id new-msg)]
    (when (:temp optm)
      (clb/create (-> new-msg :reply_markup last first :callback_query java.util.UUID/fromString)
                  user 'cloregram.handler/delete-message {:mid new-msg-id}))
    (when (and (not (:temp optm)) (not= new-msg-id (:msg-id user)))
      (u/set-msg-id user new-msg-id))))

(defn- prepare-and-send
  [type user data kbd & opts]
  (let [optm (prepare-options-map opts)
        keyboard (prepare-keyboard kbd user optm)]
    (send-to-chat type user data keyboard optm)))

;;; Public API

(defn send-message
  
  "Sends text message with content `text` and inline keyboard `kbd`  to `user`.
  Possible `opts`:

  | key     | description |
  |---------|-------------|
  | `:temp` | Sends 'temporal' message that appears with notification under 'main' one. This message will have button to delete it
  | `:markdown` | Messsage will use Markdown parse_mode"
  
  [user text kbd & opts]
  (apply prepare-and-send :message user text kbd opts))
