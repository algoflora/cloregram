(ns magic-tray-bot.schema.predicats
  (:require [datomic.api :as d]))

;; (defn user-point-valid?
;;   [db uid]
;;   (let [[point place] (d/q '[:find [?point ?place]
;;                              :in $ ?uid
;;                              :where [?uid :user/point ?point]
;;                                     (or (and [(= "point.root" (namespace ?point))]
;;                                              (not [?uid :user/place _]))
;;                                         (and [(())])))
;;                                         [?uid :user]))])
;;     (cond
;;       (and (= "point.root" (namespace (:user/point u)))
;;            (nil? (:user/place u)))
;;       true

;;       ))
;; )

