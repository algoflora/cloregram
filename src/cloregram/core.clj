(ns cloregram.core
  (:require
   [clojure.java.io :as io]
   [dialog.logger :as log]
   [integrant.core :as ig]
   [cloregram.utils :refer [deep-merge]]
   [cloregram.system.init :refer [startup shutdown!]])
  (:gen-class))

(Thread/setDefaultUncaughtExceptionHandler
 (reify Thread$UncaughtExceptionHandler
   (uncaughtException [_ thread ex]
     (log/fatal ex "Uncaught exception on" (.getName thread))
     (throw ex))))

(defn- get-conf
  [obj]
  (if-let [cfg obj]
    (-> cfg slurp ig/read-string)
    {}))

(defn run
  "Main function"
  [& args]
  (log/debug "run function args:" args)
  (let [config-arg (-> args first get-conf)
        config-user (-> "config.edn" io/resource get-conf)
        config-default (-> "default-config.edn" io/resource slurp ig/read-string)
        config (deep-merge config-default config-user config-arg)]
    (log/info "Config loaded")
    (log/debug "Config:" config)
    (.addShutdownHook (Runtime/getRuntime) (Thread. shutdown!))
    (startup config)))
