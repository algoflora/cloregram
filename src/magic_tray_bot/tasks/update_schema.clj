(ns magic-tray-bot.tasks.update-schema
    (:require [datomic.api :as d]
              [dialog.logger :as log]
              [magic-tray-bot.db :as db]
              [clojure.java.io :as io]
              [clojure.edn :as edn])
    (:gen-class))

(defn- req&->name!
  [ns]
  (log/info ns)
  (require ns)
  (name ns))

(defn debug-
  [expr]
  (log/debug expr)
  expr)

(defn update-schema
  "Updates schema with NEW data. Not removing unactual." ; TODO: Develop full update
  []
  (log/info "Updating schema...")
  (let [transf #(->> % (io/reader) (java.io.PushbackReader.) (edn/read))
        schema (->> (file-seq (io/file "./src/magic_tray_bot/schema"))
                    (filter #(= (re-find #"\.[a-zA-Z0-9]+$" (.getName %)) ".edn"))
                    (mapcat transf)
                    (vec))]
    (log/debug "Schema:" schema)
    (let [f (d/transact (db/conn) schema)]
      (log/info "Schema successfully updated with" (count (:tx-data @f)) "Datoms"))))

(defn -main
  []
  (update-schema)
  (System/exit 0))
