(ns magic-tray-bot.schema.predicats.category
  (:require [datomic.api :as d]))

(defn one-char-sign?
  [db cid]
  (let [[sign name plname] (d/q '[:find [?s ?n ?plname]
                                  :in $ ?c
                                  :where
                                  [?c :category/sign ?s]
                                  [?c :category/name ?n]
                                  [?c :category/place ?pl]
                                  [?pl :place/name ?plname]]
                                db cid)]
    (if (not= 1 (count sign))
      (format "Category's '%s' in Place '%s' sign in not one char")
      true)))

