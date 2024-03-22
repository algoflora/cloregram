(ns cloregram.core
  (:require [clojure.java.io :as io]
            [taoensso.timbre :as log]
            [integrant.core :as ig]
            [cloregram.logging]
            [cloregram.utils :as utl]
            [cloregram.utils :refer [deep-merge]]
            [cloregram.system.init :refer [startup shutdown!]]
            [cloregram.db :as db])
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

(defn run

  "Main function. Configs used overriding each other:

  - default config of cloregram framework
  - config from config.edn resource of project
  - config from .edn files or resources provided as arguments

  Detailed config reference will be provided later."
  
  [& args]
  (log/debug "Starting \"run\" function..." {:run-arguments args})
  (let [config-default    (-> "default-config.edn" io/resource get-conf)
        project-conf-path (System/getProperty "config.path" "config.prod.edn")
        config-project    (-> project-conf-path io/resource get-conf)
        config-args       (map get-conf args)
        config            (apply deep-merge config-default config-project config-args)]
    (log/info "Config loaded" {:config config})
    (.addShutdownHook (Runtime/getRuntime) (Thread. shutdown!))
    (startup config)
    (log/info "System initialized" {:system @cloregram.system.state/system})))
