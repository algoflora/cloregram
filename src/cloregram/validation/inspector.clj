(ns cloregram.validation.inspector
  (:require [clojure.test :refer [is]]
            [cloregram.utils :as utl]
            [clojure.java.io :as io]
            [com.brunobonacci.mulog :as μ]
            [fivetonine.collage.util :as clgu]
            [fivetonine.collage.core :as clg]))

(defn check-text

  "Used to check that text in text message is correct. Tests that `text` and `:text` field of `msg` are equal. Returns `msg`"

  {:changed "0.9.1"}

  [msg text]
  (is (= text (:text msg)))
  msg)

(defn check-photo

  "Used to check that photo message is correct. Tests that `caption` and `:caption` field of `msg` are equal and image in resource `expected` is equal to photo in `msg`. Returns `msg`"

  {:changed "0.9.1"}
  
  [msg caption expected]
  (is (= caption (:caption msg)))
  (let [fid     (-> msg :photo (subs 9) keyword)
        f       (-> msg fid :tempfile)
        act-img  (clgu/load-image f)
        exp-img (clgu/load-image (io/resource expected))]
    (μ/log ::check-photo :act-img act-img :exp-img exp-img)
    (is (= (.getWidth exp-img) (.getWidth act-img)))
    (is (= (.getHeight exp-img) (.getHeight act-img)))

    (is (= true (utl/img-comparer act-img exp-img))))
  msg)

(defn check-document

  "Used to check that document message is correct. Tests that `caption` and `:caption` field of `msg` are equal and byte array `content` is equal to document in `msg`. Returns `msg`"

  {:changed "0.9.1"}

  [msg caption content]
  (is (= caption (:caption msg)))
  (let [fid (-> msg :document (subs 9) keyword)
        f   (-> msg fid :tempfile)
        ba  (-> f (.length) byte-array)
        in  (java.io.FileInputStream. f)]
    (.read in ba)
    (.close in)
    (is (= (seq content) (seq ba))))
  msg)

(defn check-invoice

  "Used to check that invoice message is correct. Tests that `expected` and `:invoice` field of `msg` are equal. Returns `msg`"

  {:changed "0.9.1"}

  [msg expected]
  (is (= expected (:invoice msg)))
  msg)

(defn check-btns

  "Used to check inline keyboard layout for any kind of `msg`. Tests that buttons structure and texts in `msg` are equal to `btns`. Returns `msg`"

  {:changed "0.9.1"}

  [msg btns]
  (let [bs (->> msg :reply_markup :inline_keyboard (map #(vec (map :text %))) vec)]
    (is (= btns bs))
    msg))
