(ns cloregram.core
  (:require [clojure.java.io :as io]
            [com.brunobonacci.mulog :as μ]
            [com.brunobonacci.mulog.flakes :as f]
            [cloregram.utils :as utl]
            [taoensso.timbre :as log]
            [integrant.core :as ig]
            [cheshire.core :refer [generate-string]]
            [cloregram.logging]
            [cloregram.utils :refer [deep-merge]]
            [cloregram.system.init :refer [startup shutdown!]]
            [cloregram.system.state :refer [system]])
  (:gen-class))

(Thread/setDefaultUncaughtExceptionHandler
 (reify Thread$UncaughtExceptionHandler
   (uncaughtException [_ thread ex]
     (log/error "Uncaught Exception!" {:thread (.getName thread)
                                       :exeption ex})
     (throw ex))))

(defn- get-conf
  [obj]
  (if-let [cfg obj]
    (-> cfg slurp ig/read-string)
    {}))

;; Public API

(defn run

  "Main function. Configs used overriding each other:

  - default config of Cloregram framework
  - config from `config.dev.edn`, `config.test.edn` or `config.prod.edn` resource of project depending of current Leiningen profile
  - EDN-serialised configs from `java.io.File` files or `java.newt.URL` resources provided as arguments"
  
  [& args]
  (let [pinfo (utl/get-project-info)]
    (μ/set-global-context! {:group (:group pinfo)
                            :app-name (:name pinfo)
                            :version (:version pinfo)
                            :env "local"
                            :hostname (.getHostName (java.net.InetAddress/getLocalHost))}))
  (μ/start-publisher! {:type :console
                       :pretty? true})
  (μ/start-publisher! {:type :file-json
                       :filename "./logs/log.json"})
  (μ/log ::started)
  (log/debug "Starting \"run\" function..." {:run-arguments args})
  (let [config-default    (-> "default-config.edn" io/resource get-conf)
        project-conf-path (System/getProperty "config.path" "config.prod.edn")
        config-project    (-> project-conf-path io/resource get-conf)
        config-args       (map get-conf args)
        config            (apply deep-merge config-default config-project config-args)]
    (μ/log ::config-loaded)
    (log/info "Config loaded" {:config config})
    (.addShutdownHook (Runtime/getRuntime) (Thread. shutdown!))
    (μ/trace ::startup []
             (startup config))
    (log/info "System initialized" {:system @cloregram.system.state/system})))
