(ns magic-tray-bot.core
  (:require
   [clojure.java.io :as io]
   [dialog.logger :as log]
   [integrant.core :as ig]
   [magic-tray-bot.utils :refer [deep-merge]]
   [magic-tray-bot.app-system :as app-sys])
  (:gen-class))

(Thread/setDefaultUncaughtExceptionHandler
 (reify Thread$UncaughtExceptionHandler
   (uncaughtException [_ thread ex]
     (log/fatal ex "Uncaught exception on" (.getName thread)))))

(defn -main
  "Main function"
  [& args]
  (log/debug "Main function args:" args)
  (let [config-arg (when args (first args))
        config-obj (cond (nil? config-arg) nil
                         (string? config-arg) (io/file config-arg)
                         (instance? java.io.File config-arg) config-arg
                         (instance? java.net.URL config-arg) config-arg
                         :else (throw (IllegalArgumentException. (str "Wrong command line argument: " config-arg))))
        config-user (if config-obj (-> config-obj slurp ig/read-string) {})
        config-default (-> "config.edn" io/resource slurp ig/read-string)
        config (deep-merge config-default config-user)]
    (log/info "Config loaded")
    (log/debug "Config:" config)
    (.addShutdownHook (Runtime/getRuntime) (Thread. app-sys/shutdown!))
    (app-sys/startup config)))
