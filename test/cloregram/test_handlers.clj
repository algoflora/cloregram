(ns cloregram.test-handlers
  (:require [cloregram.api :as api]
            [cloregram.texts :refer [txt]]
            [cloregram.dynamic :refer :all]
            [cloregram.filesystem :as fs]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [com.brunobonacci.mulog :as μ]
            [nano-id.core :refer [nano-id]]
            [fivetonine.collage.util :as clgu]
            [fivetonine.collage.core :as clg]))

(defn main
  [{:keys [message]}]
  (μ/trace ::core [:core/message message]
           (api/send-message *current-user*
                             (str (:user/username *current-user*) " " (str/upper-case (:text message)))
                             [[["+" 'cloregram.test-handlers/increment {:n 0}]["-" 'cloregram.test-handlers/decrement {:n 0}]]])))

(defn increment
  [{:keys [n]}]
  (μ/trace ::increment [:increment/n n]
           (let [n (inc n)]
             (api/send-message *current-user* (txt :increment n)
                               [[{:text "+" :func 'cloregram.test-handlers/increment :args {:n n}}["-" 'cloregram.test-handlers/decrement {:n n}]]
                                [["Temp" 'cloregram.test-handlers/temp {}]]]))))

(defn decrement
  [{:keys [n]}]
  (μ/trace ::decrement [:decrement/n n]
           (let [n (dec n)]
             (api/send-message *current-user* (txt :decrement n)
                               [[["+" 'cloregram.test-handlers/increment {:n n}]["-" 'cloregram.test-handlers/decrement {:n n}]]]))))

(defonce ^:private temp-id (atom nil))

(defn temp
  [_]
  (μ/trace ::temp
           (->> (api/send-message *current-user* (txt [:temp :1]) [[[(txt [:buttons :new-text :1]) 'cloregram.test-handlers/new-temp]]] :temp)
                :message_id
                (reset! temp-id))))

(defn new-temp
  [_]
  (μ/trace ::new-temp
           (api/send-message *current-user* (txt [:temp :2]) [[[(txt [:buttons :new-text :2]) 'cloregram.test-handlers/new-temp-2]]] :temp @temp-id)))

(defn new-temp-2
  [_]
  (μ/trace ::new-temp-2
           (api/send-message *current-user* (txt [:temp :3]) nil :temp @temp-id)))

(defn photo-handler
  [{{:keys [photo]} :message}]
  (μ/trace ::photo-handler [:photo-handler/photo photo]
           (let [photo# (fn [u pss]
                          (let [file (->> pss
                                          (apply max-key :width)
                                          :file_id
                                          api/get-file)
                                name (nano-id)
                                path-in  (fs/temp-path (str name ".jpg"))
                                file-in  (.toFile path-in)
                                path-out (fs/temp-path (str name ".png"))]
                            (.renameTo file file-in)
                            (μ/log ::process-image :file file-in :image (clgu/load-image file-in))
                            (clg/with-image file-in
                              (clg/flip :horizontal)
                              (clg/flip :vertical)
                              (clgu/save (.toString path-out) :quality 1.0 ))
                            (api/send-photo *current-user* (.toFile path-out) "Flipped!" [])))]
             (if photo
               (photo# *current-user* photo)
               (api/send-message *current-user* (txt :image-expected) [] :temp)))))
