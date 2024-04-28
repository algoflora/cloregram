(ns cloregram.texts
  (:require [cloregram.impl.texts :as impl]
            [cloregram.dynamic :refer :all]))

(defn txti

  "Gets string from .EDN files in resource folder 'texts' by `path` key if `path` is a keyword or by nested `path` if `path` is a sequence and `language-code`. If there no such language key in map on `path` then config field `:bot/default-language-code` would be used. The resulting string is formatted using `args` and `clojure.core/format` function."

  {:added "0.11.0"
   :see-also "txt"}

  [language-code path & args]
  (apply impl/txti language-code path args))

(defn txt
  
  "Gets string from .EDN files in resource folder 'texts' by `path` key if `path` is a keyword or by nested `path` if `path` is a sequence and `:language-code` field of `*current-user*` dynamic variable. If there no such language key in map on `path` then config field `:bot/default-language-code` would be used. The resulting string is formatted using `args` and `clojure.core/format` function."

  {:added "0.11.0"
   :see-also "txti"}

  [path & args]
  (apply txti (:user/language-code *current-user*) path args))
