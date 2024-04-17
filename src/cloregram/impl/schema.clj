(ns ^:no-doc cloregram.impl.schema
  (:require [cloregram.impl.schema.user :refer [user]]
            [cloregram.impl.schema.callback :refer [callback]]))

(def schema (merge user callback))
