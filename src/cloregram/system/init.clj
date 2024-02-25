(ns cloregram.system.init
  (:require [integrant.core :as ig]
            [cheshire.core :refer [parse-string]]
            [dialog.logger :as log]
            [nano-id.core :refer [nano-id]]
            [ring.adapter.jetty :refer [run-jetty]]
            [datomic.api :as d]
            [telegrambot-lib.core :as tbot]
            [cloregram.system.state :refer [system]]
            [cloregram.handler :refer [main-handler]]
            [cloregram.utils :refer [api-wrap]]))

(defn startup
  [config]
  (reset! system (ig/init config))
  (log/info "Startup complete"))

(defn shutdown!
  []
  (log/info "Gracefully shutting down...")
  (ig/halt! @system)
  (log/info "Everything finished. Good bye!"))

(defmethod ig/init-key :bot/webhook-key
  [_ _]
  (when-let [key (nano-id)]
    (log/info "Webhook key created")
    (log/debug "Webhook key:" key)
    key))

(defmethod ig/init-key :bot/handler ; TODO: Check update_id
  [_ {:keys [webhook-key]}]
  (fn [req]
    (log/debug "Incoming request:" req)
    (let [headers (:headers req)
          upd (-> req :body slurp (parse-string true))]
      (if (or (= webhook-key (headers "X-Telegram-Bot-Api-Secret-Token"))
              (= webhook-key (headers (clojure.string/lower-case "X-Telegram-Bot-Api-Secret-Token"))))
        (do (main-handler upd)
            {:status 200
             :headers {"Content-Type" "application/json"}
             :body "OK"})
        {:status 403}))))

(defmethod ig/init-key :bot/https?
  [_ https?]
  https?)

(defmethod ig/init-key :bot/admins
  [_ admins]
  admins)

(defmethod ig/init-key :bot/ip
  [_ ip]
  ip)

(defmethod ig/init-key :bot/port
  [_ port]
  port)

(defmethod ig/init-key :bot/token
  [_ token]
  token)

(defmethod ig/init-key :bot/api-url
  [_ url]
  url)

(defn- adjust-opts
  [opts]
  (let [options {:host  (:ip opts)
                 :http? (not (:https? opts))
                 :ssl?  (:https? opts)
                 :join? false}]
    (cond-> options
      (not (:https? opts)) (assoc :port (:port opts))
      (:https? opts)       (assoc :port (inc (:port opts))
                                  :ssl-port (:port opts)
                                  :sni-host-check? false
                                  :keystore (or (:keystore opts) "ssl/keystore.jks")
                                  :key-password (or (:key-password opts) "ss-bot.keystorepass")))))

(defmethod ig/init-key :bot/server
  [_ {:keys [options handler]}]
  (try
    (when-let [server (run-jetty handler (adjust-opts options))]
      (log/info (format "Server started with options %s" options))
      (log/debug "Server:" server)
      server)
    (catch Exception e
      (log/error e)
      (throw e))))

(defmethod ig/halt-key! :bot/server
  [_ server]
  (.stop server)
  (log/info "Server shutted down"))

(defmethod ig/init-key :bot/instance
  [_ {:keys [api-url token webhook-key https? ip port public-key]}]
  (let [_config {:bot-token token}
        config (merge _config (if (some? api-url) {:bot-api api-url} {}))
        bot (tbot/create config)
        schema (if https? "https" "http")]
    (api-wrap tbot/set-webhook bot
              (cond-> {:content-type :multipart
                       :url (format "%s://%s:%d" schema ip port)
                       :secret_token webhook-key}
                https? (assoc :certificate (clojure.java.io/file (or public-key "ssl/cert.pem")))))
    (log/info "Webhook is set")
    (log/debug "Webhook info:" (api-wrap tbot/get-webhook-info bot))
    bot))

(defmethod ig/init-key :db/connection
  [_ {:keys [create? uri]}]
  (when create? (d/create-database uri))
  (when-let [conn (d/connect uri)]
    (log/info (format "Database connection to %s established" uri))
    conn))

(defmethod ig/halt-key! :db/connection
  [_ conn]
  (log/info "Releasing database connection...")
  (d/release conn))

(defmethod ig/init-key :project/config
  [_ config]
  (log/info "Using project config:" config)
  config)
