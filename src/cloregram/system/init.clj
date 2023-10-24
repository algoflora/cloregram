(ns cloregram.system.init
  (:require [integrant.core :as ig]
            [cheshire.core :refer [parse-string]]
            [dialog.logger :as log]
            [nano-id.core :refer [nano-id]]
            [org.httpkit.server :refer [run-server]]
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
      (if (= webhook-key (headers (clojure.string/lower-case "X-Telegram-Bot-Api-Secret-Token")))
        (do (main-handler upd)
            {:status 200
             :headers {"Content-Type" "application/json"}
             :body "OK"})
        {:status 403}))))

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

(defmethod ig/init-key :bot/server
  [_ {:keys [options handler]}]
  (when-let [server (run-server handler options)]
    (log/info (format "Server started with options %s" options))
    (log/debug "Server:" server)
    server))

(defmethod ig/halt-key! :bot/server
  [_ server]
  (server :timeout 3)
  (log/info "Server shutted down"))

(defmethod ig/init-key :bot/instance
  [_ {:keys [api-url token webhook-key ip port]}]
  (let [_config {:bot-token token}
        config (merge _config (if (some? api-url) {:bot-api api-url} {}))
        bot (tbot/create config)]
    (api-wrap tbot/set-webhook bot {:url (format "http://%s:%d" ip port)})
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
