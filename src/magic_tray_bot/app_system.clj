(ns magic-tray-bot.app-system
  (:require [integrant.core :as ig]
            [dialog.logger :as log]
            [nano-id.core :refer [nano-id]]
            [org.httpkit.server :refer [run-server]]
            [telegrambot-lib.core :as tbot]
            [magic-tray-bot.utils :refer [api-wrap]]))

(defonce ^:private system (atom nil))

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
  (let [key (nano-id)]
    (log/info "Webhook key created")
    (log/debug "Webhook key:" key)
    key))

(defmethod ig/init-key :bot/handler
  [_ {:keys [webhook-key]}]
  (fn [req]
    (let [headers (:headers req)
          body (:body req)]
      (log/debug "Incoming request:" req)
      (if (= webhook-key (headers "X-Telegram-Bot-Api-Secret-Token"))
        {:status 200
         :headers {"Content-Type" "application/json"}
         :body "OK"}
        {:status 403}))))

(defmethod ig/init-key :bot/ip
  [_ ip]
  ip)

(defmethod ig/init-key :bot/token
  [_ token]
  token)

(defmethod ig/init-key :bot/api-url
  [_ url]
  url)

(defmethod ig/init-key :bot/server
  [_ {:keys [options handler]}]
  (let [server (run-server handler options)]
    (log/info "Server started")
    (log/debug "Server:" server)
    server))

(defmethod ig/init-key :bot/instance
  [_ {:keys [api-url token webhook-key ip]}]
  (let [_config {:bot-token token}
        config (merge _config (if (some? api-url) {:bot-api api-url} {}))
        bot (tbot/create config)]
    (api-wrap tbot/set-webhook bot {:url (str "http://" ip)})
    (log/info "Webhook is set")
    (log/debug "Webhook info:" (api-wrap tbot/get-webhook-info bot))))

(defmethod ig/halt-key! :bot/server
  [_ server]
  (server :timeout 3)
  (log/info "Server shutted down"))
