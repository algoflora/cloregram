(ns cloregram.middleware
  (:require [clojure.string :as str]
            [com.brunobonacci.mulog :as μ]
            [com.brunobonacci.mulog.flakes :refer [read-method]]))

(defn wrap-exception [handler]
  (fn [request]
    (try (handler request)
         (catch Exception e
           (let [data (assoc (if (instance? clojure.lang.ExceptionInfo e)
                               (.getData e) {})
                             :exception-class (str (class e))
                             :exception-cause (.getCause e)
                             :stack-trace     (str/join "\n" (mapv str (.getStackTrace e))))]
             {:status 200
              :body {:ok false
                     :description (.getMessage e)}})))))

(defn wrap-tracking-requests
  "tracks api events with μ/log."
  ([handler] (wrap-tracking-requests nil handler))
  ([prefix handler]
   (fn [req]
     (μ/trace (keyword (str *ns*) (str (name prefix) "tracking-http-request"))
       ;; add here all the key/value pairs for tracking event only
              {:pairs [:uri              (get req :uri)
                       :request-method   (get req :request-method)
                       :content-type     (get-in req [:headers "content-type"])
                       :content-encoding (get-in req [:headers "content-encoding"])]
        ;; out of the response capture the http status code.
        :capture (fn [{:keys [status]}] {:http-status status})}

       ;; call the request handler
       (handler req)))))

(defn pass-mulog-trace
  [handler]
  (fn [req]
    (let [ctx    (μ/local-context)
          root   (some-> req (get-in [:headers "mulog-pass-root-trace"]) read-method)
          parent (some-> req (get-in [:headers "mulog-pass-parent-trace"]) read-method)
          ctx#   (cond-> ctx
                   (some? root)   (assoc :mulog/root-trace root)
                   (some? parent) (assoc :mulog/parent-trace parent))]
      (μ/with-context ctx#
        (handler req)))))
