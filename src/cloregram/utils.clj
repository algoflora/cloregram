(ns cloregram.utils
  (:require [cloregram.system.state :refer [bot]]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [resauce.core :as res]
            [dialog.logger :as log]))

(defn dbg
  [x]
  (log/debug "DBG:" x)
  x)

(defn inf
  [x]
  (log/info "INF:" x)
  x)

(defn api-wrap-
  [api-f-sym bot & args]
  (log/debug (format "Calling (%s %s)" api-f-sym (str/join " " args)))
  (let [api-f (ns-resolve (find-ns 'telegrambot-lib.core) api-f-sym)
        resp  (apply api-f bot args)
        ok    (true? (:ok resp))]
    (when (not ok)
      (log/debug "RESP" resp)
      (throw (ex-info "API response error!" {:response resp
                                             :call api-f-sym
                                             :arguments args})))
    (log/debug (format "%s response is OK: %s" api-f resp))
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
      (log/error "Callback not resolved:" sym))))

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
