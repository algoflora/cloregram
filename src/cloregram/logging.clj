(ns cloregram.logging
  (:require [clojure.string :as str]
            [clojure.walk :as walk]
            [taoensso.timbre :as timbre]
            [cheshire.core :refer [generate-string]]
            [cloregram.utils :as utl]))

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
    (walk/postwalk (fn [x] (if (map? x) (into {} (map f x)) x)) m)))(defn- keywordize
  [m])

(defn custom-appender
  [data]
  (let [{:keys [level ?err ?file ?line vargs]} data
        message (first vargs)
        additional-data (next vargs)
        log-entry (-> (utl/get-project-info)
                      (merge {:time (str (java.time.Instant/now))
                              :file ?file
                              :line ?line
                              :level (name level)
                              :message (-> message type str)
                              :data (map #(-> % type str) (when additional-data (vec additional-data)))
                              :error ?err}))]
    (spit "./logs/logfile.json" (str (generate-string log-entry) "\n") :append true)))

(timbre/merge-config!
 {:appenders {:custom-appender {:enabled? true
                                :fn custom-appender}}})

(timbre/info "Logging initiated with Timbre" timbre/*config*)


;; {:output-opts nil,
;;  :hash_ #object[clojure.lang.Delay 0xcd381e4 {:status :pending,
;;                                               :val nil}],
;;  :instant #inst "2024-03-10T19:33:29.871-00:00",
;;  :spying? nil,
;;  :config {:min-level :debug,
;;           :ns-filter #{"*"},
;;           :middleware [],
;;           :timestamp-opts {:pattern :iso8601,
;;                            :locale :jvm-default,
;;                            :timezone :utc},
;;           :output-fn #object[taoensso.timbre$default_output_fn 0x2fab6393 "taoensso.timbre$default_output_fn@2fab6393"],
;;           :appenders {:println {:enabled? true,
;;                                 :fn #object[taoensso.timbre.appenders.core$println_appender$fn__7829 0x41e5664e "taoensso.timbre.appenders.core$println_appender$fn__7829@41e5664e"]},
;;                       :custom-appender {:enabled? true,
;;                                         :fn #object[cloregram.logging$custom_appender 0x960a64a "cloregram.logging$custom_appender@960a64a"]}},
;;           :_init-config {:loaded-from-source [:default],
;;                          :compile-time-config {:min-level nil, :ns-pattern "*"}}},
;;  :vargs ["Everything finished. Good bye!"],
;;  :output_ #object[clojure.lang.Delay 0x631f378d {:status :pending, :val nil}],
;;  :msg_ #object[clojure.lang.Delay 0x51f31f7c {:status :pending, :val nil}],
;;  :?file "cloregram/system/init.clj",
;;  :hostname_ #object[clojure.lang.Delay 0x1bf885c6 {:status :ready,
;;                                                    :val "mbpm2-avbo.local"}],
;;  :error-level? false,
;;  :appender {:enabled? true,
;;             :fn #object[cloregram.logging$custom_appender 0x960a64a "cloregram.logging$custom_appender@960a64a"]},
;;  :appender-id :custom-appender,
;;  :?ns-str "cloregram.system.init",
;;  :level :info,
;;  :msg-type :p,
;;  :output-fn #object[taoensso.timbre$protected_fn$fn__8284 0x2adedc6d "taoensso.timbre$protected_fn$fn__8284@2adedc6d"],
;;  :?err nil,
;;  :timestamp_ #object[clojure.lang.Delay 0x73d354b2 {:status :ready, :val "2024-03-10T19:33:29.871Z"}],
;;  :context nil,
;;  :?line 22,
;;  :?meta nil,
;;  :?msg-fmt nil,
;;  :?column 3}
