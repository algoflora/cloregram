(ns cloregram.test-handlers
  (:require [cloregram.api :as api]
            [cloregram.utils :as utl]
            [clojure.string :as str]))

(defn common
  [{:keys [user message]}]
  (api/send-message user
                    (str (:user/username user) " " (str/upper-case (:text message)))
                    [[["+" 'cloregram.test-handlers/increment [0]]["-" 'cloregram.test-handlers/decrement [0]]]]))

(defn increment
  [n user]
  (let [n (inc n)]
    (api/send-message user (format "Incremented: %d" n)
                      [[["+" 'cloregram.test-handlers/increment [n]]["-" 'cloregram.test-handlers/decrement [n]]]
                       [["Temp" 'cloregram.test-handlers/temp []]]])))

(defn decrement
  [n user]
  (let [n (dec n)]
    (api/send-message user (format "Decremented: %d" n)
                    [[["+" 'cloregram.test-handlers/increment [n]]["-" 'cloregram.test-handlers/decrement [n]]]])))

(defn temp
  [user]
  (api/send-message user "Temp message" [] :temp))