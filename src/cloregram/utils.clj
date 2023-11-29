(ns cloregram.utils
  (:require [clojure.string :as str]
            [dialog.logger :as log]))

(defn dbg
  [x]
  (log/debug "DBG:" x)
  x)

(defn api-wrap
  [api-f & args]
  (log/debug (format "Calling (%s %s)" api-f (str/join " " args)))
  (let [resp (apply api-f args)
        ok (:ok resp)
        desc (:description resp)]
    (when (not ok)
      (throw (ex-info "API response error" {:call api-f
                                            :description desc
                                            :response resp})))
    (log/debug (format "%s response is OK: %s" api-f resp))
    (:result resp)))

(defn deep-merge
  "Recursively merges maps."
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

(defn get-project-info
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
