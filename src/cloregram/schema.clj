(ns cloregram.schema
  (:require [cloregram.schema.user :refer [user]]
            [cloregram.schema.callback :refer [callback]]))

(def schema (merge user callback))
