(ns cloregram.logging
  (:require [clojure.string :as str]
            [clojure.walk :as walk]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.core :refer [println-appender]]
            [cheshire.core :refer [generate-string]]
            [cheshire.generate :refer [add-encoder]]
            [cloregram.utils :as utl]))

(add-encoder Object (fn [obj jsonGenerator] (.writeString jsonGenerator (prn-str obj))))

(defn- my-keywordize-keys
  [m]
  (let [f (fn [[k v]] (if (string? k)
                        [(-> k
                             (str/lower-case)
                             (str/replace " " "-")
                             (keyword))
                         v]
                        [k v]))]
    ;; only apply to maps
    (walk/postwalk (fn [x] (if (map? x) (into {} (map f x)) x)) m)))

(defn- transform-additional-data
  [ad]
  (cond
    (map? ad) ad

    (and (coll? ad) (<= 1 (count ad)))
    (transform-additional-data (first ad))

    (seq? ad) (vec ad)
    (some? ad) ad
    :else nil))

(defn- generate-log-entry
  [data]
  (let [{:keys [msg-type level ?err ?file ?line ?ns-str msg_ vargs hostname_]} data
        is-p? (= :p msg-type)
        message (if is-p? (first vargs) @msg_)
        additional-data (when is-p? (-> vargs next transform-additional-data my-keywordize-keys))]
    (-> (utl/get-project-info)
        (merge {:time    (str (java.time.Instant/now))
                :host    @hostname_
                :file    ?file
                :line    ?line
                :logger  ?ns-str
                :level   (name level)
                :message (cond-> {:message message
                                  :data additional-data}
                           (some? ?err) (assoc :error ?err)
                           true generate-string)})
        (my-keywordize-keys))))

(defn custom-json-appender
  [data]
  (let [log-entry (-> data generate-log-entry generate-string (str "\n"))]
    (spit "./logs/logs.json" log-entry :append true)))

;; (defn custom-edn-appender
;;   [data]
;;   (let [log-entry (-> data generate-log-entry prn-str)]
;;     (spit "./logs/logs.edn" log-entry :append true)))

(def original-output-fn (-> timbre/*config* :output-fn))

(timbre/merge-config!
 {:appenders

  {:custom-json-appender {:enabled? true
                          :async? true
                          :fn custom-json-appender}

   ;; :custom-edn-appender {:enabled? true
   ;;                       :fn custom-edn-appender}

   :println {:enabled? true
             :min-level :info
             :output-fn (fn [data]
                          (original-output-fn (update
                                               data
                                               :vargs
                                               #(if (= :p (:msg-type data)) (take 1 %) %))))}}})

(timbre/info "Logging initiated with Timbre" timbre/*config*)
