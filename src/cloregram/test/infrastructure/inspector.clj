(ns cloregram.test.infrastructure.inspector
  (:require [clojure.test :refer [is]]
            [cloregram.utils :as utl]
            [cloregram.system.state :refer [system]]))

(defn check-text
  "Used to check that text in text message is correct. Tests that `text` and `:text` field of `msg` are equal."
  [msg text]
  (is (or (= (str (-> @system :bot/decorations :major :text) text) (:text msg))
          (= (str (-> @system :bot/decorations :minor :text) text) (:text msg))))
  msg)

(defn check-document
  "Used to check that document message is correct. Tests that `caption` and `:caption` field of `msg` are equal and byte array `content` is equal to document in `msg`."
  [msg caption content]
  (is (or (= (str (-> @system :bot/decorations :major :text) caption) (:caption msg))
          (= (str (-> @system :bot/decorations :minor :text) caption) (:caption msg))))
  (let [f  (-> msg :document :tempfile)
        ba (-> f (.length) byte-array)
        in (java.io.FileInputStream. f)]
    (.read in ba)
    (.close in)
    (is (= (seq content) (seq ba))))
  msg)

(defn check-invoice
  "Used to check that invoice message is correct. Tests that `expected` and `:invoice` field of `msg` are equal."
  [msg expected]
  (is (= expected (:invoice msg)))
  msg)

(defn check-btns
  "Used to check inline keyboard layout for any kind of `msg`. Tests that buttons structure and texts in `msg` are equal to `btns`"
  [msg btns]
  (let [bs (-> msg :reply_markup :inline_keyboard utl/simplify-reply-markup)]
    (is (= btns bs))
    msg))
