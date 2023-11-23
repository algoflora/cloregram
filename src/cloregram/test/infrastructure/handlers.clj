(ns cloregram.test.infrastructure.handlers
  (:require [cloregram.api :as api]
            [cloregram.utils :as utl]))

(defn common
  [{:keys [user message]}]
  (api/send-message user
                    (str (:user/username user) " " (str/upper-case (:text message)))
                    [[["+" 'cloregram.test.infrastructure.handlers/increment [0]]["-" 'cloregram.test.infrastructure.handlers/decrement [0]]]]))

(defn increment
  [{:keys [n user]}]
  (let [n (inc n)]
    (api/send-message user (format "Incremented: %d" n)
                      [[["+" 'cloregram.test.infrastructure.handlers/increment [n]]["-" 'cloregram.test.infrastructure.handlers/decrement [n]]]
                       [["Temp" 'cloregram.test.infrastructure.handlers/temp []]]])))

(defn decrement
  [{:keys [n user]}]
  (let [n (dec n)]
    (api/send-message user (format "Decremented: %d" n)
                    [[["+" 'cloregram.test.infrastructure.handlers/increment [n]]["-" 'cloregram.test.infrastructure.handlers/decrement [n]]]])))

(defn temp
  [{:keys [user]}]
  (api/send-message user "Temp message" [] :temp))
