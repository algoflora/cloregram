(ns cloregram.api
  (:require [cloregram.system.state :refer [bot]]
            [cloregram.utils :as utl]
            [cloregram.callbacks :as clb]
            [cloregram.users :as u]
            [cheshire.core :refer [generate-string parse-string]]
            [telegrambot-lib.core :as tbot]
            [taoensso.timbre :as log]))

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
    (assoc :callback_data (str (clb/create user (:func kmap) (:args kmap))))

    true (dissoc :func)
    true (dissoc :args)))

(defn- prepare-keyboard
  [kbd user optm]
  (let [mapf #(cond-> % (and (vector? %) (= 2 (count %))) (conj {}) true (create-key user))]
    {:inline_keyboard
     (cond->> kbd
       true (mapv #(mapv mapf %))
       (:temp optm) (#(conj % [{:text "✖️"
                                :callback_data (str (java.util.UUID/randomUUID))}])))}))

(defn- create-temp-delete-callback
  [user new-msg]
  (clb/create (-> new-msg :reply_markup :inline_keyboard last first :callback_data java.util.UUID/fromString)
              user 'cloregram.handler/delete-message {:mid (:message_id new-msg)}))

(defn- to-edit?
  [optm user] (and (not (:temp optm)) (some? (:user/msg-id user)) (not= 0 (:user/msg-id user))))

(defn- prepare-arguments-map
  [argm kbd optm user]
  (cond-> argm
    true                 (assoc :chat_id (:user/id user))
    (not-empty kbd)      (assoc :reply_markup kbd)
    (:markdown optm)     (assoc :parse_mode "Markdown")
    (to-edit? optm user) (assoc :message_id (:user/msg-id user))))

(defmulti ^:private send-to-chat (fn [& args] (identity (first args))))

(defmethod send-to-chat :message
  [_ user text kbd optm]
  (let [argm (prepare-arguments-map {:text text} kbd optm user)
        func (if (to-edit? optm user)
               tbot/edit-message-text tbot/send-message)
        new-msg (utl/api-wrap func (bot) argm)
        new-msg-id (:message_id new-msg)]
    (when (:temp optm)
      (create-temp-delete-callback user new-msg))
    (when (and (not (:temp optm)) (not= new-msg-id (:msg-id user)))
      (u/set-msg-id user new-msg-id))))

(defmethod send-to-chat :file
  [_ user data kbd optm]
  (let [user-mp (update user :user/id str)
        argm (update (prepare-arguments-map {:content-type :multipart
                                             :caption (:caption data)
                                             :document (-> data :path (java.io.File.))}
                                            kbd optm user-mp)
                     :reply_markup generate-string)
        new-msg (utl/api-wrap tbot/send-document (bot) argm)]
    (create-temp-delete-callback user new-msg)))

(defmethod send-to-chat :invoice
  [_ user data kbd optm]
  (let [argm (prepare-arguments-map data kbd optm user)
        new-msg (utl/api-wrap tbot/send-invoice (bot) argm)]
    (create-temp-delete-callback user new-msg)))

(defn- prepare-and-send
  [type user data kbd & opts]
  (let [optm (prepare-options-map opts)
        keyboard (prepare-keyboard kbd user optm)]
    (send-to-chat type user data keyboard optm)))

;;; Public API

(defn send-message
  
  "Sends text message with content `text` and inline keyboard `kbd` to `user`.
  Possible `opts`:

  | key     | description |
  |---------|-------------|
  | `:temp` | Sends 'temporal' message that appears with notification under 'main' one. This message will have button to delete it in the end
  | `:markdown` | Messsage will use Markdown parse_mode"
  
  [user text kbd & opts]
  (apply prepare-and-send :message user text kbd opts))


(defn send-document

  "Sends file in `path` as a temporary messaage with caption `caption` and inline keyboard `kbd` to `user`.
  Possible `opts`:

  | key         | description |
  |-------------|-------------|
  | `:markdown` | Messsage will use Markdown parse_mode"

  {:added "0.4"}

  [user path caption kbd & opts]
  (apply prepare-and-send :file user {:path path :caption caption} kbd :temp opts))


(defn send-invoice

  "Sends invoice as 'temporal' message with inline keyboard `kbd` to `user`. Keyboard will have payment button with `pay-text` in the beginning and button to delete it in the end.  
  Description of `data` map (all keys required):
  | key               | description |
  |-------------------|-------------|
  | `:title`          | Product name, 1-32 characters
  | `:description`    | Product description, 1-255 characters
  | `:payload`        | Bot-defined invoice payload, 1-128 bytes. This will not be displayed to the user, use for your internal processes.
  | `:provider_token` | Payment provider token
  | `:currency`       | Three-letter ISO 4217 currency code
  | `:prices`         | Price breakdown, a JSON-serialized list of components (e.g. product price, tax, discount, delivery cost, delivery tax, bonus, etc.). Each component have to be map with keys `:label` (string) and `:amount` (integer price of the product in the smallest units of the currency)"

  {:added "0.5"}

  [user data pay-text kbd]
  (prepare-and-send :invoice user data (vec (cons [{:text pay-text :pay true}] kbd)) :temp))
