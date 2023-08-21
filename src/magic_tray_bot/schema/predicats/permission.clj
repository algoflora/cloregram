(ns magic-tray-bot.schema.predicats.permission
  (:require [datomic.api :as d]
            [magic-tray-bot.db :as db]))

(defn categorized?
  [db p-id]
  (let [[{cats :permission/categories} catzd?]
        (d/q '[:find (pull p-id [:permission/categories]) ?catzd?
               :in $ p-id
               :where [p-id :permission/type ?pt]
               [?pt :permission.type/categorized ?catzd?]])]))
