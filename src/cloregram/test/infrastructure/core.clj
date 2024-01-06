(ns cloregram.test.infrastructure.core
  (:require [dialog.logger :as log]
            [integrant.core :as ig]
            [clojure.java.io :as io]
            [org.httpkit.server :refer [run-server]]
            [compojure.core :refer [defroutes POST context]]
            [ring.middleware.json :refer [wrap-json-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [cheshire.core :refer [generate-string]]
            [cloregram.test.infrastructure.handler :refer [handler]]))

(defn- json-reponse-body-middleware
  [handler]
  (fn [req]
    (let [resp (handler req)]
      (update resp :body generate-string))))

(defn- logging-middleware
  [handler]
  (fn [req]
    (do (log/debug "Incoming request:" req)
        (handler req))))

(defn- create-routes
  [path]
  (defroutes routes
    (context path []
             (POST "/:endpoint" [] #(handler (:params %)))))
  (-> routes
      logging-middleware
      wrap-keyword-params
      wrap-json-params
      wrap-multipart-params
      json-reponse-body-middleware))

(defmethod ig/init-key :test/server
  [_ {:keys [url bot-token]}]
  (let [u (io/as-url url)
        host (.getHost u)
        port (.getPort u)
        path (.getPath u)
        server (run-server (create-routes (str path bot-token))
                           {:ip host :port port})]
    (log/info "Server started")
    server))

(defmethod ig/halt-key! :test/server
  [_ server]
  (server :timeout 3)
  (log/info "Testing server shutted down"))
