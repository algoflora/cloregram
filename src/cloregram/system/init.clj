(ns cloregram.system.init
  (:require [clojure.string :as str]
            [integrant.core :as ig]
            [cheshire.core :refer [parse-string]]
            [taoensso.timbre :as log]
            [nano-id.core :refer [nano-id]]
            [ring.adapter.jetty :refer [run-jetty]]
            [datalevin.core :as d]
            [clojure.java.io :as io]
            [cloregram.db :as db]
            [telegrambot-lib.core :as tbot]
            [cloregram.system.state :refer [system]]
            [cloregram.handler :refer [main-handler]]
            [cloregram.utils :as utl]))

(defn startup
  [config]
  (reset! system (ig/init config)))

(defn shutdown!
  []
  (log/info "Gracefully shutting down...")
  (ig/halt! @system)
  (shutdown-agents)
  (log/info "Everything finished. Good bye!"))

(defmethod ig/init-key :bot/webhook-key
  [_ _]
  (when-let [key (nano-id)]
    (log/info "Webhook key created")
    key))

(defmethod ig/init-key :bot/handler ; TODO: Check update_id
  [_ {:keys [webhook-key]}]
  (fn [req]
    (log/debug "Incoming webhook request" {:webhook-request req})
    (let [headers (:headers req)
          upd (-> req :body slurp (parse-string true))]
      (if (or (= webhook-key (headers "X-Telegram-Bot-Api-Secret-Token"))
              (= webhook-key (headers (clojure.string/lower-case "X-Telegram-Bot-Api-Secret-Token"))))
        (do (main-handler upd)
            {:status 200
             :headers {"Content-Type" "application/json"}
             :body "OK"})
        {:status 403
         :body "Forbidden!"}))))

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
                                  :keystore (or (:keystore opts) "./ssl/keystore.jks")
                                  :key-password (or (:keystore-password opts) "cloregram.keystorepass")))))

(defmethod ig/init-key :bot/server
  [_ {:keys [options handler]}]
  (try
    (when-let [server (run-jetty (utl/wrap-exception handler) (adjust-opts options))]
      (log/info "Webhook server started" {:server-options options
                                          :server server})
      server)
    (catch Exception e
      (log/error "Error starting webhook server!" {:error e})
      (throw e))))

(defmethod ig/halt-key! :bot/server
  [_ server]
  (.stop server)
  (log/info "Server shutted down"))

(defmethod ig/init-key :bot/instance
  [_ {:keys [api-url token webhook-key https? ip port certificate]}]
  (let [_config {:bot-token token}
        config (merge _config (if (some? api-url) {:bot-api api-url} {}))
        bot (tbot/create config)
        schema (if https? "https" "http")]
    (utl/api-wrap- 'set-webhook bot (cond-> {:content-type :multipart
                                             :url (format "%s://%s:%d" schema ip port)
                                             :secret_token webhook-key}
                                      https? (assoc :certificate (clojure.java.io/file
                                                                  (or certificate "./ssl/cert.pem")))))
    (log/info "Webhook is set" {:webhook-info (utl/api-wrap- 'get-webhook-info bot)})
    bot))

(defn- delete-directory-recursive
  "Recursively delete a directory."
  [^java.io.File file]
  (when (.isDirectory file)
    (run! delete-directory-recursive (.listFiles file)))
  (try (io/delete-file file)
       (catch Exception e (println (.getMessage e)))))

(defmethod ig/init-key :db/connection
  [_ {:keys [uri clear?]}]
  (when clear?
    (delete-directory-recursive (io/file uri)))
  (let [options {:validate-data? true
                 :closed-schema? true}
        schema (db/get-full-schema)
        conn (d/get-conn uri schema options)]
    (log/info "Datalevin database connection established" {:database-uri uri
                                                           :database-schema (d/schema conn)
                                                           :database-options (d/opts conn)})
    conn))

(defmethod ig/halt-key! :db/connection
  [_ conn]
  (log/info "Releasing Datalevin database connection...")
  (d/close conn))

(defmethod ig/init-key :project/config
  [_ config]
  (log/info "Project config loaded" {:project-config config})
  config)
