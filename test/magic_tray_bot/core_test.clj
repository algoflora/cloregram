(ns magic-tray-bot.core-test
  (:require [clojure.test :refer :all]
            [datomic.api :as d]
            [dialog.logger :as log]
            ; [magic-tray-bot.core :refer :all]
            [magic-tray-bot.db :as db]
            [magic-tray-bot.fixtures :as fix]))

(use-fixtures :each fix/use-in-memory-db fix/use-actual-schema)

(deftest uri-test
  (testing "Count datoms"
    (let [initial-datoms-number (-> (d/datoms (db/get-db) :eavt)
                                    (seq)
                                    (count))]
      (is (= 361 initial-datoms-number)))))
