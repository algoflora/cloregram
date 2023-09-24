(ns magic-tray-bot.test.infrastructure.core
  (:require [dialog.logger :as log]
            [integrant.core :as ig]
            [clojure.java.io :as io]
            [org.httpkit.server :refer [run-server]]
            [compojure.core :refer [defroutes POST context]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [magic-tray-bot.test.infrastructure.handler :refer [handler]]))

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
      wrap-multipart-params))

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
