(ns magic-tray-bot.schema.predicats.user
  (:require [datomic.api :as d]))

(defn user-point-valid?
  [db u-id]
  (let [[u-name point place] (d/pull db [:user/username :user/point :user/place] u-id)
        point-nm (namespace point)
        in-place? (some? place)]
    (cond (not (re-matches #"^point($|\.[a-z0-9-\.]+$)" point-nm))
          (format "Wrong Point '%s' namespace for User %s" point u-name)

          (and (= "point" point-nm) in-place?)
          (format "Root Point '%s' in Place '%s' for User %s" point place u-name)

          (and (not= "point" point-nm) (not in-place?))
          (format "Non-root Point '%s' not in Place for User %s" point place u-name)

          :else true)))

