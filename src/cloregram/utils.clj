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
    (log/debug (format "%s response is OK: %s" api-f desc))
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
