(ns cloregram.test.infrastructure.core
  (:require [taoensso.timbre :as log]
            [integrant.core :as ig]
            [clojure.java.io :as io]
            [org.httpkit.server :refer [run-server]]
            [compojure.core :refer [defroutes GET POST context]]
            [ring.middleware.json :refer [wrap-json-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [cheshire.core :refer [generate-string]]
            [cloregram.test.infrastructure.handler :refer [handler]]
            [cloregram.test.infrastructure.state :as state]
            [cloregram.middleware :as mw]))

(defn- json-reponse-body-middleware
  [handler]
  (fn [req]
    (let [resp (handler req)]
      (update resp :body generate-string))))

(defn- logging-middleware
  [handler]
  (fn [req]
    (log/debug "Incoming request to virtual Telegram API server" {:virtual-tg-api-request req})
    (let [resp (handler req)]
      (log/debug "Outgoing response from virtual Telegram API server" {:virtual-tg-api-response resp})
      resp)))

(defn- create-routes
  [path bot-token]
  (let [api-path  (format "%s%s/:endpoint" path bot-token)
        file-path (format "%sfile/bot%s/:file_path" path bot-token)]
    (defroutes routes
      (POST api-path [] (fn [req] (handler (:params req))))
      (GET file-path [] (fn [req] (if-let [file (->> req
                                                  :params
                                                  :file_path
                                                  (.decode (java.util.Base64/getDecoder))
                                                  String.
                                                  (get @state/files))]
                                           {:status 200
                                            :headers {"Content-Type" "application/octet-stream"
                                                      "Content-Length" (.length file)}
                                            :body (io/input-stream file)}
                                           {:status 404
                                            :headers {"Content-Type" "text/plain"}
                                            :body "File not found!"}))))
    (-> routes
        logging-middleware
        wrap-keyword-params
        wrap-json-params
        wrap-multipart-params
        mw/wrap-exception
        json-reponse-body-middleware)))

(defmethod ig/init-key :test/server
  [_ {:keys [url bot-token]}]
  (let [u (io/as-url url)
        host (.getHost u)
        port (.getPort u)
        path (.getPath u)
        server (run-server (create-routes path bot-token)
                           {:ip host :port port})]
    (log/info "Testing server started" {:server server})
    server))

(defmethod ig/halt-key! :test/server
  [_ server]
  (server :timeout 3)
  (log/info "Testing server shut down"))
