(ns cloregram.test-handlers
  (:require [cloregram.api :as api]
            [cloregram.filesystem :as fs]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [com.brunobonacci.mulog :as μ]
            [nano-id.core :refer [nano-id]]
            [fivetonine.collage.util :as clgu]
            [fivetonine.collage.core :as clg]))

(defn core
  [{:keys [user message]}]
  (μ/trace ::core [:core/user user :core/message message]
           (api/send-message user
                             (str (:user/username user) " " (str/upper-case (:text message)))
                             [[["+" 'cloregram.test-handlers/increment {:n 0}]["-" 'cloregram.test-handlers/decrement {:n 0}]]])))

(defn increment
  [{:keys [n user]}]
  (μ/trace ::increment [:increment/n n :increment/user user]
           (let [n (inc n)]
             (api/send-message user (format "Incremented: %d" n)
                               [[{:text "+" :func 'cloregram.test-handlers/increment :args {:n n}}["-" 'cloregram.test-handlers/decrement {:n n}]]
                                [["Temp" 'cloregram.test-handlers/temp {}]]]))))

(defn decrement
  [{:keys [n user]}]
  (μ/trace ::decrement [:decrement/n n :decrement/user user]
           (let [n (dec n)]
             (api/send-message user (format "Decremented: %d" n)
                               [[["+" 'cloregram.test-handlers/increment {:n n}]["-" 'cloregram.test-handlers/decrement {:n n}]]]))))

(defonce ^:private temp-id (atom nil))

(defn temp
  [{:keys [user]}]
  (μ/trace ::temp [:temp/user user]
           (->> (api/send-message user "Temp message" [[["New text" 'cloregram.test-handlers/new-temp]]] :temp)
                :message_id
                (reset! temp-id))))

(defn new-temp
  [{:keys [user]}]
  (μ/trace ::new-temp [:new-temp/user user]
           (api/send-message user "New temp message" [[["New text 2" 'cloregram.test-handlers/new-temp-2]]] :temp @temp-id)))

(defn new-temp-2
  [{:keys [user]}]
  (μ/trace ::new-temp-2 [:new-temp-2-user user]
           (api/send-message user "New temp message 2" nil :temp @temp-id)))

(defn photo-handler
  [{user :user {:keys [photo]} :message}]
  (μ/trace ::photo-handler [:photo-handler/user user :photo-handler/photo photo]
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
                            (api/send-photo user (.toFile path-out) "Flipped!" [])))]
             (if photo
               (photo# user photo)
               (api/send-message user "Image expected!" [] :temp)))))
