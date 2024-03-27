(ns cloregram.middleware
  (:require [clojure.string :as str]
            [com.brunobonacci.mulog :as μ]))

(defn wrap-exception [handler]
  (fn [request]
    (try (handler request)
         (catch Exception e
           (let [data (assoc (if (instance? clojure.lang.ExceptionInfo e)
                               (.getData e) {})
                             :exception-class (str (class e))
                             :exception-cause (.getCause e)
                             :stack-trace     (str/join "\n" (mapv str (.getStackTrace e))))]
             (μ/log ::http-request-failed :exception e)
             {:status 200
              :body {:ok false
                     :description (.getMessage e)}})))))

(defn wrap-tracking-events
  "tracks api events with μ/log."
  [handler]
  (fn [req]
    (μ/with-context
      {:uri            (get req :uri)
       :request-method (get req :request-method)}

      ;; track the request duration and outcome
      (μ/trace ::http-request
        ;; add here all the key/value pairs for tracking event only
        {:pairs [:content-type     (get-in req [:headers "content-type"])
                 :content-encoding (get-in req [:headers "content-encoding"])]
         ;; out of the response capture the http status code.
         :capture (fn [{:keys [status]}] {:http-status status})}

        ;; call the request handler
        (handler req)))))
