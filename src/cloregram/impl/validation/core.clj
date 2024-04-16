(ns cloregram.impl.validation.core
  (:require [com.brunobonacci.mulog :as μ]
            [integrant.core :as ig]
            [clojure.java.io :as io]
            [org.httpkit.server :refer [run-server]]
            [compojure.core :refer [defroutes routes GET POST]]
            [ring.middleware.json :refer [wrap-json-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [cheshire.core :refer [generate-string]]
            [cloregram.impl.validation.handler :refer [handler]]
            [cloregram.impl.validation.state :refer [files]]
            [cloregram.impl.middleware :as mw]))

(defn- json-reponse-body-middleware
  [handler]
  (fn [req]
    (let [resp (handler req)]
      (update resp :body generate-string))))

(defn- create-api-route
  [path bot-token]
  (let [api-path  (format "%s%s/:endpoint" path bot-token)]
    (defroutes route
      (POST api-path []
            (fn [req]
              (μ/trace ::validation-api-request
                       {:pairs [:validation-api-request/request (select-keys req [:uri :params :request-method :remote-addr :content-length :scheme])]
                        :capture (fn [resp] {:validation-api-request/response resp})}
                       (handler (:params req))))))
    (-> route
        mw/parse-json-for-multipart
        wrap-keyword-params
        wrap-json-params
        wrap-multipart-params
        mw/wrap-exception
        json-reponse-body-middleware
        mw/pass-mulog-trace)))

(defn- create-files-route
  [path bot-token]
  (let [file-path (format "%sfile/bot%s/:filepath" path bot-token)]
    (defroutes route
      (GET file-path []
           (fn [req]
             (μ/trace ::validation-file-request
                      {:pairs [:validation-file-request/request (select-keys req [:uri :params :request-method :remote-addr :content-length :scheme])]
                       :capture (fn [resp] {:validation-file-request/response resp})}
                      (if-let [file (->> req
                                         :params
                                         :filepath
                                         (.decode (java.util.Base64/getDecoder))
                                         String.
                                         (get @files))]
                        {:status 200
                         :headers {"Content-Type" "application/octet-stream"
                                   "Content-Length" (.length file)}
                         :body (io/input-stream file)}
                        {:status 404
                         :headers {"Content-Type" "text/plain"}
                         :body "File not found!"})))))
    (-> route
        wrap-keyword-params
        mw/wrap-exception
        mw/pass-mulog-trace)))

(defmethod ig/init-key :test/server
  [_ {:keys [url bot-token]}]
  (let [u (io/as-url url)
        host (.getHost u)
        port (.getPort u)
        path (.getPath u)
        handler (routes
                 (create-files-route path bot-token)
                 (create-api-route path bot-token))
        server (run-server handler {:ip host :port port})]
    (μ/log ::validation-server-started :validation-server-started/server server)
    server))

(defmethod ig/halt-key! :test/server
  [_ server]
  (server :timeout 3)
  (μ/log ::validation-server-shut-down))
