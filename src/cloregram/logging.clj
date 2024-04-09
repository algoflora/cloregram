(ns cloregram.logging
  (:require [cloregram.utils :refer [get-project-info]]
            [com.brunobonacci.mulog :as μ]
            [where.core :refer [where]]
            [clojure.string :as str]
            [clojure.java.io :as io]))

(defonce ^:private publishers (atom nil))

;; (defn transform-add-timestamp
;;   [events]
;;   (map #(assoc % "@timestamp" (:mulog/timestamp %)) events))

(def ^:private time-format (java.text.SimpleDateFormat. ))

(defn- format-timestamp [timestamp]
  (let [formatter (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm:ss.SSS")]
    (.format formatter (java.util.Date. timestamp))))

(def ^:private human-readable-time (partial map (fn [event]
                                                  (let [timestamp (:mulog/timestamp event)
                                                        readable-time (format-timestamp timestamp)]
                                                    (assoc event :mulog/time readable-time)))))

(def ^:private errors-filter (partial filter #(contains? % :exception)))

(def ^:private publisher-errors-filter (partial filter (where :mulog/event-name :in? [:mulog/publisher-error :myapp/invalid-json-value])))

(defn- capitalize-words 
  "Capitalize every word in a string"
  [s]
  (->> (str/split (str s) #"\b") 
       (map str/capitalize)
       str/join))

(def ^:private console-log-transformer
  (partial
   map #(-> %
            (select-keys [:mulog/timestamp :mulog/event-name :exception])
            (update :mulog/timestamp (fn [ts] (java.time.Instant/ofEpochMilli ts)))
            (update :mulog/event-name (fn [en] (str (-> en name (str/replace #"-" " ") (capitalize-words)) " (" (-> en namespace) ")"))))))

(def ^:private publishers-config [
                                  #_{:type :console
                                   :pretty? true
                                   :transform console-log-transformer}
                           
                                  {:type :simple-file
                                   :filename "./logs/log.mulog"}

                                  {:type :simple-file
                                   :filename "./logs/last.mulog"
                                   :transform human-readable-time}
                                  
                                  {:type :simple-file
                                   :filename "./logs/errors.mulog"
                                   :transform (comp human-readable-time errors-filter)}
                                  
                                  {:type :simple-file
                                   :filename "./logs/publishers-errors.mulog"
                                   :transform (comp human-readable-time publisher-errors-filter)}
                                  
                                  {:type :zipkin
                                   :url "http://127.0.0.1:9411"
                                   :max-items 5000
                                   :publish-delay 5000}])

(defn start-publishers!
  []
  (let [pinfo (get-project-info)]
    (io/delete-file "./logs/last.mulog" true)
    (io/delete-file "./logs/errors.mulog" true)
    (io/delete-file "./logs/publishers-errors.mulog" true)
    (μ/set-global-context! {:group (:group pinfo)
                            :app-name (:name pinfo)
                            :version (:version pinfo)
                            :env "local"
                            :hostname (.getHostName (java.net.InetAddress/getLocalHost))})
    (reset! publishers (mapv (fn [p]
                               (let [f (μ/start-publisher! p)]
                                 (μ/log ::publisher-started :type (:type p) :publisher-stop-fn f)
                                 f))
                             publishers-config))))

(defn stop-publishers!
  []
  (μ/log ::publishers-stop)
  (Thread/sleep 5000)
  (mapv (fn [f] (f)) @publishers))
