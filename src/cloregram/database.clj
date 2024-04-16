(ns cloregram.database
  (:require [cloregram.impl.database :as impl]
            [cloregram.impl.state :refer [system]]
            [datalevin.core :as d]
            [com.brunobonacci.mulog :as μ]))

(defn conn

  "Returns Datalevin database connection"

  {:changed "0.9.1"}

  []
  (:db/connection @system))

(defn db

  "Returns current Datalevin database state"

  {:changed "0.9.1"}
  
  []
  (d/db (conn)))

(defn load-data
  
  "Loads initial data from .edn files in resources folder 'data'. Re-write if something exists"

  {:changed "0.9.1"}
  
  []
  (let [user-data (impl/read-project-data)]
    (μ/log ::initial-data-uploaded :initial-data-uploaded/datoms (:tx-data (d/transact! (conn) user-data)))))
