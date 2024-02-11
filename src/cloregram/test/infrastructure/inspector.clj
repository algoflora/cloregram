(ns cloregram.test.infrastructure.inspector
  (:require [clojure.test :refer [is]]
            [cloregram.utils :as utl]
            [dialog.logger :as log]))

(defn check-text
  [msg text]
  (is (= text (:text msg)))
  msg)

(defn check-document
  [msg caption content]
  (is (= caption (:caption msg)))
  (is (= content (:document msg)))
  msg)

(defn check-btns
  [msg btns]
  (let [bs (-> msg :reply_markup utl/simplify-reply-markup)]
    (is (= btns bs))
    msg))
