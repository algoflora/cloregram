(ns magic-tray-bot.test.fixtures
  (:require  [clojure.java.io :as io]
             [clojure.core.async :as a]
             [dialog.logger :as log]
             [telegrambot-lib.http :refer [request gen-url client parse-resp ex->parsed-resp]]
             [telegrambot-lib.json :as json]
             [magic-tray-bot.test.infrastructure.core]
             [magic-tray-bot.core :refer [-main]]))

(defmethod request false
  ([this path]
   (log/debug "REQUEST-2-1" this path)
   (request this path nil))

  ([this path content]
   (log/debug "REQUEST-2-2" this path content)
   (let [url (gen-url this path)
         req {:body (json/generate-str content) :content-type :json}]
     (try
       (let [resp (client :post url req)]
         (parse-resp resp))
       (catch Throwable ex
         (ex->parsed-resp ex))))))

(defn use-test-environment
  [body]
  (-main (io/resource "test-config.edn"))
  (body))
