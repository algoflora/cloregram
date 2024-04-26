(ns ^:no-doc cloregram.impl.api
  (:require [cloregram.impl.state :refer [bot system]]
            [cloregram.impl.callbacks :as clb]
            [cloregram.impl.users :as u]
            [cloregram.filesystem :as fs]
            [nano-id.core :refer [nano-id]]
            [cheshire.core :refer [generate-string]]
            [clojure.walk :as walk]
            [org.httpkit.client :as http]
            [com.brunobonacci.mulog :as μ]))

(defn api-wrap-
  [api-f-sym bot & args]
  (μ/trace ::telegram-api-call
           {:pairs [:telegram-api-call/method api-f-sym :telegram-api-call/arguments (into () args)]
            :capture (fn [resp] {:telegram-api-call/response resp})}
           (let [api-f (ns-resolve (find-ns 'telegrambot-lib.core) api-f-sym)
                 resp  (apply api-f bot args)
                 ok    (true? (:ok resp))]
             (when (not ok)
               (throw (ex-info "API response error" {:method api-f-sym
                                                     :arguments (into () args)
                                                     :response resp})))
             (:result resp))))

(defn api-wrap
  [api-f-sym & args]
  (apply api-wrap- api-f-sym (bot) args))

(defn- check-opt
  [opts opt]
  (boolean (some #{opt} opts)))

(defn- prepare-options-map
  [opts]
  (let [to-edit-msg-id (->> opts (filter number?) (first))]
    (-> {}
        (assoc :temp           (or (check-opt opts :temp) (some? to-edit-msg-id)))
        (assoc :markdown       (check-opt opts :markdown))
        (assoc :to-edit-msg-id to-edit-msg-id))))

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
  (when kbd
    (let [mapf #(cond-> % (and (vector? %) (= 2 (count %))) (conj {}) true (create-key user))]
      {:inline_keyboard
       (cond->> kbd
         true (mapv #(mapv mapf %))
         (:temp optm) (#(conj % [{:text "✖️"
                                  :callback_data (str (java.util.UUID/randomUUID))}])))})))

(defn- create-temp-delete-callback
  [user new-msg]
  (-> new-msg
      :reply_markup
      :inline_keyboard
      last
      first
      :callback_data
      java.util.UUID/fromString
      (clb/create user 'cloregram.handler/delete-message {:mid (:message_id new-msg)})))

(defn- set-callbacks-message-id
  [user msg]
  (clb/set-new-message-ids
   user
   (:message_id msg)
   (->> msg
        :reply_markup :inline_keyboard flatten
        (mapv #(some-> % :callback_data java.util.UUID/fromString)) (filterv some?))))

(defn- to-edit?
  [optm user]
  (when (and (some? (:mdg-id user)) (= (:to-edit-msg-id optm) (:msg-id user)))
    (throw (ex-info "Prohibited way to edit Main Message!" {})))
  (if (:temp optm)
    (some? (:to-edit-msg-id optm))
    (and (some? (:user/msg-id user))
         (not= 0 (:user/msg-id user)))))

(defn- prepare-arguments-map
  [argm kbd optm user]
  (cond-> argm
    true                 (assoc :chat_id (:user/id user))
    (some? kbd)          (assoc :reply_markup kbd)
    (:markdown optm)     (assoc :parse_mode "Markdown")
    (to-edit? optm user) (assoc :message_id (or (:to-edit-msg-id optm) (:user/msg-id user)))))

(defmulti ^:private send-to-chat (fn [& args] (identity (first args))))

(defn- create-media
  [type data]
  {:pre? [(map? data)]}
  {:type (name type)
   :caption (:caption data)
   :media (:file data)})

(defn- prepare-to-multipart
  [args-map]
  (let [files     (atom {})
        args-map# (walk/postwalk
                   (fn [x]
                     (cond (instance? java.io.File x)
                           (let [file-id (keyword (str "fid" (nano-id 10)))]
                             (swap! files assoc file-id x)
                             (str "attach://" (name file-id)))
                           (number? x) (str x)
                           :else x))
                   (assoc args-map :content-type :multipart))]
    (cond-> args-map#
      (contains? args-map# :media)        (update :media generate-string)
      (contains? args-map# :reply_markup) (update :reply_markup generate-string)
      true (merge @files))))

(defn- send-new-media-to-chat
  [type args-map media]
  (let [media-is-file? (instance? java.io.File (:media media))
        method         (symbol (str "send-" (name type)))
        args-map#      (cond-> (merge args-map media)
                         true (dissoc :media)
                         true (assoc type (:media media))
                         media-is-file? (prepare-to-multipart))]
    (api-wrap method args-map#)))

(defn- edit-media-in-chat
  [type args-map media]
  (let [media-is-file? (instance? java.io.File (:media media))
        args-map#      (cond-> args-map
                         true (assoc :media media)
                         media-is-file? (prepare-to-multipart))]
    (try (api-wrap 'edit-message-media args-map#)
         (catch clojure.lang.ExceptionInfo e
           (send-new-media-to-chat type args-map media)))))

(defn- send-media-to-chat
  [type user data kbd optm]
  (let [media    (create-media type data)
        args-map (prepare-arguments-map {} kbd optm user)
        new-msg  (if (to-edit? optm user)
                   (edit-media-in-chat type args-map media)
                   (send-new-media-to-chat type args-map media))]
    (when (some? kbd)
      (create-temp-delete-callback user new-msg))
    (set-callbacks-message-id user new-msg)
    new-msg))

(defmethod send-to-chat :photo
  [& args]
  (apply send-media-to-chat args))

(defmethod send-to-chat :document
  [& args]
  (apply send-media-to-chat args))

(defmethod send-to-chat :invoice
  [_ user data kbd optm]
  (let [argm (prepare-arguments-map data kbd optm user)
        new-msg (api-wrap 'send-invoice argm)]
    (create-temp-delete-callback user new-msg)
    (set-callbacks-message-id user new-msg)
    new-msg))

(defn- send-message-to-chat
  [argm to-edit??]
  (let [func-sym (if to-edit?? 'edit-message-text 'send-message)]
    (try (api-wrap func-sym argm)
         (catch clojure.lang.ExceptionInfo e
           (api-wrap 'send-message argm)))))

(defmethod send-to-chat :message
  [_ user text kbd optm]
  (let [argm       (prepare-arguments-map {:text text} kbd optm user)
        new-msg    (send-message-to-chat argm (to-edit? optm user))
        new-msg-id (:message_id new-msg)]
    (when (and (some? kbd) (:temp optm))
      (create-temp-delete-callback user new-msg))
    (when (and (not (:temp optm)) (not= new-msg-id (:msg-id user)))
      (u/set-msg-id user new-msg-id))
    (set-callbacks-message-id user new-msg)
    new-msg))

(defn prepare-and-send
  [type user data kbd & opts]
  (let [optm (prepare-options-map opts)
        keyboard (prepare-keyboard kbd user optm)]
    (send-to-chat type user data keyboard optm)))

(defn get-file
  [file-id]
  (let [uri (->> (api-wrap 'get-file file-id)
                 :file_path
                 (format "%sfile/bot%s/%s"
                         (or (:bot/api-url @system) "https://api.telegram.org/")
                         (:bot/token @system)))
        bis (μ/trace ::http-get {:pairs [:http-get/uri uri]
                                 :capture (fn [resp]
                                            {:http-get/response resp})}
                     (-> uri http/get deref :body))
        file (-> (nano-id) fs/temp-path .toFile)
        fos (java.io.FileOutputStream. file)]
    (try
      (.transferTo bis fos)
      (finally
        (.close fos)))
    file))

(defn delete-message
  [user mid]
  (api-wrap 'delete-message {:chat_id (:user/id user)
                             :message_id mid})
  (clb/delete user mid))
