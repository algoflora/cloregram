(ns cloregram.test-handlers
  (:require [cloregram.api :as api]
            [cloregram.filesystem :as fs]
            [clojure.string :as str]
            [taoensso.timbre :as log]
            [nano-id.core :refer [nano-id]]
            [fivetonine.collage.util :as clgu]
            [fivetonine.collage.core :as clg]))

(defn core
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

(defonce ^:private temp-id (atom nil))

(defn temp
  [{:keys [user]}]
  (->> (api/send-message user "Temp message" [[["New text" 'cloregram.test-handlers/new-temp]]] :temp)
       :message_id
       (reset! temp-id)))

(defn new-temp
  [{:keys [user]}]
  (api/send-message user "New temp message" [[["New text 2" 'cloregram.test-handlers/new-temp-2]]] :temp @temp-id))

(defn new-temp-2
  [{:keys [user]}]
  (api/send-message user "New temp message 2" nil :temp @temp-id))

(defn send-edit
  [{:keys [user message]}]
  (let [msg-id (-> message :text Integer/parseInt)]
    (api/send-message user "Temp message" nil :temp msg-id)))

(defn photo-handler
  [{user :user {:keys [photo]} :message}]
  (let [photo# (fn [u pss]
                 (let [file (->> pss
                                 (apply max-key :width)
                                 :file_id
                                 api/get-file)
                       path (fs/temp-path (str (nano-id) ".png"))]
                   (clg/with-image file
                     (clg/flip :horizontal)
                     (clg/flip :vertical)
                     (clgu/save path :quality 1.0 ))
                   #_(api/send-photo user path)))]
    (if photo
      (photo# user photo)
      (api/send-message user "Image expected!" [] :temp))))
