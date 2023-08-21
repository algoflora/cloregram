(ns magic-tray-bot.schema.product
  (:require [magic-tray-bot.db :as db]))

(def schema [{:db/ident :product/name
              :db/valueType :db.type/string
              :db/cardinality :db.cardinality/one
              :db/unique :db.unique/identity
              :db/doc "Product's name"}

             {:db/ident :product/category
              :db/valueType :db.type/ref
              :db/cardinality :db.cardinality/one
              :db/doc "Product's `Category`"}

             {:db/ident :product/price
              :db/valueType :db.type/bigdec
              :db/cardinality :db.cardinality/one
              :db/doc "Product's price. Two fractional digits."}

             {:db/ident :product/consumables
              :db/valueType :db.type/tuple
              :db/tupleTypes [:db.type/ref :db.type/bigint]
              :db/cardinality :db.cardinality/many
              :db/doc "Products ingridients in tuple format [:consumable/id amount-:bigint]"}

             {:db/ident :product/validate
              :db.entity/attrs [:product/name :product/category :product/price]}])
