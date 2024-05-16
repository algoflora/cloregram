(ns cloregram.dynamic)

(def ^{:dynamic true
       :doc "User from whom the current Update came"
       :added "0.11.0"}
  *current-user* nil)

(def ^{:dynamic true
       :doc "ID of Message from which the current callback query came"
       :added "0.11.0"}
  *from-message-id* nil)

(defmacro ^:private executor-code
  []
  (let [version-string (System/getProperty "java.version")
        [major _ _] (map #(Integer/parseInt %) (clojure.string/split version-string #"\."))]
    (if (<= 21 major)
      `(java.util.concurrent.Executors/newVirtualThreadPerTaskExecutor)
      `clojure.lang.Agent/soloExecutor)))

(def ^{:dynamic true
       :doc "ExecutorService binded to result of `java.util.concurrent/newVirtualThreadPerTaskExecutor` if Java 21+ is used or `clojure.lang.Agent/soloExecutor` otherwise"
       :added "0.12.1"}
  *executor* (executor-code))
