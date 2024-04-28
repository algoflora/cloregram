(ns cloregram.impl.texts
  (:require [cloregram.impl.state :refer [system]]
            [cloregram.impl.resources :refer [read-resource-dir]]
            [com.brunobonacci.mulog :as μ]))

(def ^:private texts (into {} (->> (read-resource-dir "texts")
                                   (map #(into {} [%]))
                                   (apply merge))))

(defmulti txti (fn [_ path & _] (seqable? path)))

(defmethod txti false
  [lang path & args]
  (apply txti lang (vector path) args))

(defmethod txti true
  [lang path & args]
  (let [path# (vec path)
        lang-map (get-in texts path#)]
    (when (not (map? lang-map))
      (throw (ex-info "Not a map in texts by given path!" {:path path
                                                           :lang-map lang-map})))
    (apply format
           (or ((keyword lang) lang-map) ((:bot/default-language-code @system) lang-map)) args)))
