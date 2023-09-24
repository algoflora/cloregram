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

(Thread/setDefaultUncaughtExceptionHandler
 (reify Thread$UncaughtExceptionHandler
   (uncaughtException [_ thread ex]
     (log/fatal ex "Uncaught exception on" (.getName thread)))))

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
        bot (tbot/create config)
        wh-resp (tbot/set-webhook bot {:url (str "https://" ip "/")})]
       (if (true? wh-resp)
         (do 
           (log/info "Webhook is set")
           (log/debug "Webhook info:" (tbot/get-webhook-info bot))
           bot)
         (throw (java.util.ServiceConfigurationError. (str wh-resp))))))

(defmethod ig/halt-key! :bot/server
  [_ server]
  (server :timeout 3)
  (log/info "Server shutted down")
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
  (let [config-arg (when args (first args))
        config-obj (cond (nil? config-arg) nil
                         (string? config-arg) (io/file config-arg)
                         (instance? java.io.File config-arg) config-arg
                         (instance? java.net.URL config-arg) config-arg
                         :else (throw (IllegalArgumentException. (str "Wrong command line argument: " config-arg))))
        config-user (if config-obj (-> config-obj slurp ig/read-string) {})
        config-default (-> "config.edn" io/resource slurp ig/read-string)
        config (deep-merge config-default config-user)]
    (.addShutdownHook (Runtime/getRuntime) (Thread. shut-down))
    (log/info "Config loaded")
    (log/debug "Config:" config)
    (reset! system (ig/init config))
    (log/info "All systems started")))

