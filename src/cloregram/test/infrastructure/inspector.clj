(ns cloregram.test.infrastructure.inspector
  (:require [clojure.test :refer [is]]
            [cloregram.utils :as utl]))

(defn check-text
  [msg text]
  (is (= text (:text msg)))
  msg)

(defn check-document
  [msg caption content]
  (is (= caption (:caption msg)))
  (let [f  (-> msg :document :tempfile)
        ba (-> f (.length) byte-array)
        in (java.io.FileInputStream. f)]
    (.read in ba)
    (.close in)
    (is (= (seq content) (seq ba))))
  msg)

(defn check-invoice
  [msg expected]
  (is (= (select-keys msg [:title :description :payload :provider_token :currency :prices]) expected))
  msg)

(defn check-btns
  [msg btns]
  (let [bs (-> msg :reply_markup utl/simplify-reply-markup)]
    (is (= btns bs))
    msg))
