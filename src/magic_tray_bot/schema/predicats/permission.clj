(ns magic-tray-bot.schema.predicats.permission
  (:require [datomic.api :as d]))

(defn categorized?
  [db p-id]
  (let [[{cats :permission/categories} ptc uname plname catzd?]
        (d/q '[:find (pull p-id [:permission/categories]) ?ptc ?uname ?plname ?catzd?
               :in $ p-id
               :where
               [p-id :permission/user ?u]
               [?u :user/username ?uname]
               [p-id :permission/place ?pl]
               [?pl :place/name ?plname]
               [p-id :permission/type ?pt]
               [?pt :permission.type/code ?ptc]
               [?pt :permission.type/categorized ?catzd?]])]
    (cond (and catzd? (nil? cats))
          (format "Permission of categorized Permission Type %s of User %s in Place '%s' have no :permission/categories attribute" ptc uname plname)

          (and (not catzd? (some? cats)))
          (format "Permission of uncategorized Permission Type %s of User %s in Place '%s' have the :permission/categories attribute" ptc uname plname)

          :else true)))
