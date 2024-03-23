(ns cloregram.utils
  (:require [cloregram.system.state :refer [bot]]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [resauce.core :as res]
            [taoensso.timbre :as log]))

(defn dbg
  ([x] (dbg nil x))
  ([msg x]
   (log/debug msg x)
   x))

(defn api-wrap-
  [api-f-sym bot & args]
  (log/debug "API method calling" {:method api-f-sym
                                   :arguments args})
  (let [api-f (ns-resolve (find-ns 'telegrambot-lib.core) api-f-sym)
        resp  (apply api-f bot args)
        ok    (true? (:ok resp))]
    (when (not ok)
      (throw (ex-info "API response error" {:method api-f-sym
                                            :arguments args
                                            :response resp})))
    (log/debug "Response is OK" {:method api-f-sym
                                 :arguments args
                                 :response resp})
    (:result resp)))

(defn api-wrap
  [api-f-sym & args]
  (apply api-wrap- api-f-sym (bot) args))

(defn deep-merge
  "Recursively merges maps"
  [& maps]
  (letfn [(m [& xs]
            (if (some #(and (map? %) (not (record? %))) xs)
              (apply merge-with m xs)
              (last xs)))]
    (reduce m maps)))

(defn keys-hyphens->underscores ; NOT recursive!
  [m]
  (into {} (map (fn [[k v]] [(-> k name (.replace \- \_) keyword) v]) m)))

(defn simplify-reply-markup
  [reply-markup]
  (vec (map #(vec (map :text %)) reply-markup)))

(defn msg->str
  [msg]
  (let [msg (-> msg
                (select-keys [:text :reply_markup])
                (update :reply_markup simplify-reply-markup))]
    (format "%s\t%s" (:text msg) (:reply_markup msg))))

(defn username
  [user]
  (or (:user/username user) (str "id" (:user/id user))))

(defmacro get-project-info
  []
  (let [[_ ga version] (read-string (slurp "project.clj"))
        [ns name version] [(namespace ga) (name ga) version]]
    {:group ns
     :name name
     :version version}))

(defn resolver
  [sym]
  (let [ns (-> sym namespace symbol)
        nm (-> sym name symbol)]
    (require ns)
    (if-let [resolved (ns-resolve ns nm)]
      resolved
      (log/error "Callback not resolved" {:callback-symbol sym}))))

(defn- read-resource [resource-url]
  (with-open [stream (io/input-stream resource-url)]
    (-> stream
        io/reader
        java.io.PushbackReader. edn/read)))

(defn read-resource-dir
  [dir]
  (when-let [resources (some-> dir io/resource res/url-dir)]
    (->> resources
         (filter #(str/ends-with? % ".edn"))
         (mapcat read-resource))))

(defn wrap-exception [handler]
  (fn [request]
    (try (handler request)
         (catch Exception e
           (let [data (assoc (if (instance? clojure.lang.ExceptionInfo e)
                               (.getData e) {})
                             :exception-class (str (class e))
                             :exception-cause (.getCause e)
                             :stack-trace     (str/join "\n" (mapv str (.getStackTrace e))))]
             (log/error (.getMessage e) data)
             {:status 200
              :body {:ok false
                     :description (.getMessage e)}})))))

(def ^:private temp-root
  (let [arr (into-array (concat (map val (get-project-info)) [(str (.getTime (java.util.Date.)))]))
        path (java.nio.file.Paths/get "/tmp" arr)]
    (-> path .toFile .mkdirs)
    path))

(defn ^java.nio.file.Path temp-path
  [path]
  (.resolve temp-root path))
