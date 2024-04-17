(ns ^:no-doc cloregram.impl.init
  (:require [integrant.core :as ig]
            [cheshire.core :refer [parse-string]]
            [com.brunobonacci.mulog :as μ]
            [nano-id.core :refer [nano-id]]
            [ring.adapter.jetty :refer [run-jetty]]
            [datalevin.core :as d]
            [clojure.java.io :as io]
            [telegrambot-lib.core :as tbot]
            [cloregram.impl.database :as db]
            [cloregram.impl.state :refer [system]]
            [cloregram.impl.handler :refer [main-handler]]
            [cloregram.impl.middleware :as mw]
            [cloregram.impl.api :refer [api-wrap-]]
            [cloregram.logging :refer [stop-publishers!]]))

(defn startup
  [config]
  (reset! system (ig/init config)))

(defn shutdown!
  []
  (μ/trace ::sutting-down
           (ig/halt! @system))
  (println "Everything finished. Good bye!")
  (stop-publishers!)
  (shutdown-agents))

(defmethod ig/init-key :bot/webhook-key
  [_ _]
  (nano-id))

(defmethod ig/init-key :bot/handler ; TODO: Check update_id
  [_ {:keys [webhook-key]}]
  (fn [req]
    (let [headers (:headers req)
          upd (-> req :body slurp (parse-string true))]
      
      (if (or (= webhook-key (headers "X-Telegram-Bot-Api-Secret-Token"))
              (= webhook-key (headers (clojure.string/lower-case "X-Telegram-Bot-Api-Secret-Token"))))
        (μ/trace ::main-handler [:update upd]
                 (main-handler upd)
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

(def ^:private wrap-tracking-events-bot (partial mw/wrap-tracking-requests "bot-"))

(defmethod ig/init-key :bot/server
  [_ {:keys [options handler]}]
  (let [opts (adjust-opts options)]
    (μ/trace ::webhook-server-start
             {:pairs [:webhook-server-start/options opts]
              :capture (fn [server] {:webhook-server-start/server server})}
             (run-jetty (-> handler
                            mw/wrap-exception
                            wrap-tracking-events-bot)
                        opts))))

(defmethod ig/halt-key! :bot/server
  [_ server]
  (μ/trace ::server-shutdown
           (.stop server)))

(defn- μ-headers-fn
  []
  (let [ctx (μ/local-context)]
    {"mulog-pass-root-trace" (:mulog/root-trace ctx) 
     "mulog-pass-parent-trace" (:mulog/parent-trace ctx)}))

(defmethod ig/init-key :bot/instance
  [_ {:keys [api-url token webhook-key https? ip port certificate]}]
  (let [_config {:bot-token token
                 :headers μ-headers-fn}
        config (merge _config (if (some? api-url) {:bot-api api-url}))
        bot (tbot/create config)
        schema (if https? "https" "http")]
    (api-wrap- 'set-webhook bot (cond-> {:content-type :multipart
                                         :url (format "%s://%s:%d" schema ip port)
                                         :secret_token webhook-key}
                                  https? (assoc :certificate (clojure.java.io/file
                                                              (or certificate "./ssl/cert.pem")))))
    (μ/log ::webhook-is-set :webhook-info (api-wrap- 'get-webhook-info bot))
    bot))

(defn- del-dir-rec
  "Recursively delete a directory."
  [^java.io.File file]
  (when (.isDirectory file)
    (run! del-dir-rec (.listFiles file)))
  (try (io/delete-file file)
       (catch Exception e (println (.getMessage e)))))

(defmethod ig/init-key :db/connection
  [_ {:keys [uri clear?]}]
  (when clear?
    (del-dir-rec (io/file uri)))
  (let [options {:validate-data? true
                 :closed-schema? true}
        schema (db/get-full-schema)]
    (μ/trace ::database-connection
             {:pairs [:database-connection/uri uri]
              :capture (fn [c]
                         {:database-connection/schema (d/schema c)
                          :database-connection/oprions (d/opts c)})}
             (d/get-conn uri schema options))))

(defmethod ig/halt-key! :db/connection
  [_ conn]
  (d/close conn))

(defmethod ig/init-key :project/config
  [_ config]
  (μ/log ::project-config-loaded :project-config config)
  config)
