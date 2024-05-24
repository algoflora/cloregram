(ns cloregram.core
  (:require [cloregram.logging :refer [start-publishers!]]
            [clojure.java.io :as io]
            [com.brunobonacci.mulog :as μ]
            [integrant.core :as ig]
            [cloregram.utils :refer [deep-merge]]
            [cloregram.impl.init :refer [startup shutdown!]]
            [cloregram.impl.state :refer [system]]
            [cloregram.impl.validation.core])
  (:gen-class))

(defn set-exceptions-handler
  []
  (Thread/setDefaultUncaughtExceptionHandler
   (reify Thread$UncaughtExceptionHandler
     (uncaughtException [_ thread ex]
       (μ/log ::uncaught-exception :exception ex :thread (.getName thread))
       (shutdown!)
       (println (format "Uncaught %s: %s" (type ex) (.getMessage ex)))))))

(set-exceptions-handler)

(defn- get-conf
  [obj]
  (if-let [cfg obj]
    (-> cfg slurp ig/read-string)
    {}))

;; Public API

(defn config

  "Returns value (or nil) from project config nested map, using chain of `keys`"

  {:changed "0.9.1"}

  [& keys]
  (get-in (:project/config @system) keys))

(defn run

  "Main function. Configs used overriding each other:

  - default config of Cloregram framework
  - config from `config.dev.edn`, `config.test.edn` or `config.prod.edn` resource of project depending of current Leiningen profile
  - EDN-serialised configs from `java.io.File` files or `java.newt.URL` resources provided as arguments"
  
  [& args]
  (μ/log ::started)
  (start-publishers!)
  (μ/trace ::run [:run-arguments args]
           (let [config-default    (-> "default-config.edn" io/resource get-conf)
                 project-conf-path (System/getProperty "config.path" "config.prod.edn")
                 config-project    (-> project-conf-path io/resource get-conf)
                 config-args       (map get-conf args)
                 config            (apply deep-merge config-default config-project config-args)]
             (μ/log ::config-loaded :config config)
             (.addShutdownHook (Runtime/getRuntime) (Thread. shutdown!))
             (μ/trace ::startup []
                      (startup config))
             (μ/log ::system-initialized))))
