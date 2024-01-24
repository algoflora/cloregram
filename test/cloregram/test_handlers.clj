(ns cloregram.test-handlers
  (:require [cloregram.api :as api]
            [cloregram.utils :as utl]
            [clojure.string :as str]))

(defn common
  [{:keys [user message]}]
  (api/send-message user
                    (str (:user/username user) " " (str/upper-case (:text message)))
                    [[["+" 'cloregram.test-handlers/increment {:n 0}]["-" 'cloregram.test-handlers/decrement {:n 0}]]]))

(defn increment
  [{:keys [n user]}]
  (let [n (inc n)]
    (api/send-message user (format "Incremented: %d" n)
                      [[{:text "+" :func 'cloregram.test-handlers/increment :args {:n n}}["-" 'cloregram.test-handlers/decrement {:n n}]]
                       [["Temp" 'cloregram.test-handlers/temp {}]]])))

(defn decrement
  [{:keys [n user]}]
  (let [n (dec n)]
    (api/send-message user (format "Decremented: %d" n)
                    [[["+" 'cloregram.test-handlers/increment {:n n}]["-" 'cloregram.test-handlers/decrement {:n n}]]])))

(defn temp
  [{:keys [user]}]
  (api/send-message user "Temp message" [] :temp))
