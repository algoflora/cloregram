(ns magic-tray-bot.fixtures
  (:require  [clojure.test :as t]
             [datomic.api :as d]
             [dialog.logger :as log]
             [magic-tray-bot.db :as db]
             [magic-tray-bot.tasks.update-schema :refer [update-schema]]))

(def ^:private mem-db-uri "datomic:mem://magic-tray-dev-test")

(defn use-in-memory-db
  [body]
  (with-redefs [db/uri mem-db-uri]
    (d/delete-database db/uri)
    (assert (d/create-database db/uri) "Error while creating in-memory database!")
    (body)
    (assert (d/delete-database db/uri) "Error while deleting in-memory database!")))

(defn use-actual-schema
  [body]
  (assert (= db/uri mem-db-uri) "Actual database is not the in-memory test one!")
  (update-schema)
  (body))
