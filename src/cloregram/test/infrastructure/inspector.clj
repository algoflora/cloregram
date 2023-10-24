(ns cloregram.test.infrastructure.inspector
  (:require [clojure.test :refer [is]]
            [cloregram.utils :as utl]))

(defn check-text
  [msg text]
  (is (= text (:text msg)))
  msg)

(defn check-btns
  [msg btns]
  (let [bs (-> msg :reply_markup utl/simplify-reply-markup)]
    (is (= bs btns))
    msg))
