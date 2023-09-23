(ns magic-tray-bot.core
  (:require
   [clojure.java.io :as io]
   [dialog.logger :as log]
   [nano-id.core :refer [nano-id]]
   [integrant.core :as ig]
   [org.httpkit.server :refer [run-server]]
   [magic-tray-bot.utils :refer [deep-merge]]
   [telegrambot-lib.core :as tbot])
  (:gen-class))

(defonce system (atom nil))

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
      (log/debug "Request headers:" headers)
      (when (= webhook-key (headers "X-Telegram-Bot-Api-Secret-Token"))
        {:status 200
         :headers {"Content-Type" "application/json"}
         :body "OK"}))))

(defmethod ig/init-key :bot/ip
  [_ ip]
  ip)

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
    (when (and (nil? api-url)
               (tbot/set-webhook bot {:url (str "https://" ip "/")}))
      (log/info "Webhook is set")
      (log/debug "Webhook info:" (tbot/get-webhook-info bot)))
    bot))

(defmethod ig/halt-key! :bot/server
  [_ server]
  (server :timeout 3)
  (log/debug "Server shutted down")
  (log/debug "Server:" server))

(defn- shut-down
  []
  (log/info "Gracefully shutting down...")
  (ig/halt! @system)
  (log/info "Everything finished. Good bye!"))

(defn -main
  "Main function"
  [& args]
  (log/debug "Main function args:" args)
  (let [config-path (when args (first args))
        config-user (if config-path (-> config-path io/file slurp ig/read-string) {})
        config-default (-> "config.edn" io/resource slurp ig/read-string)
        config (deep-merge config-default config-user)]
    (.addShutdownHook (Runtime/getRuntime) (Thread. shut-down))
    (log/info "Config loaded")
    (log/debug "Config:" config)
    (reset! system (ig/init config))
    (log/info "All systems started")))

