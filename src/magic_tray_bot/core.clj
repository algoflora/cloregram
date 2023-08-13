(ns magic-tray-bot.core
  (:require [telegrambot-lib.core :as tbot]
            [dialog.logger :as log]

            [magic-tray-bot.config :as config]
            [magic-tray-bot.main-handler :as handler])

  (:gen-class))

(def token "6193405054:AAF41i2G-NEV_1QGG6nZlaJQkokRpirCBQk")

(def mybot (tbot/create token))

;; LONG POLLING

(defonce update-id (atom nil))

(defn set-id!
  "Sets the update id to process next as the the passed in `id`."
  [id]
  (reset! update-id id))

(defn poll-updates
  "Long poll for recent chat messages from Telegram."
  ([bot]
   (poll-updates bot nil))

  ([bot offset]
   (let [resp (tbot/get-updates bot {:offset offset
                                     :timeout config/timeout})]
     (if (contains? resp :error)
       (log/error "tbot/get-updates error:" (:error resp))
       resp))))

(defn lp-app
  "Retrieve and process chat messages."
  [bot]
  (log/info "Bot service started")

  (loop []
    (let [updates (poll-updates bot @update-id)
          messages (:result updates)]

      ;; Check all messages, if any, for commands/keywords.
      (doseq [msg messages]
        (handler/handle bot msg) ; your fn that decides what to do with each message.

        ;; Increment the next update-id to process.
        (-> msg
            :update_id
            inc
            set-id!))

      ;; Wait a while before checking for updates again.
      (Thread/sleep config/sleep))
    (recur)))

;; MAIN

(defn -main
  "Main function"
  [& args]
  (log/set-level! :debug)
  (log/set-level! "org.apache.http" :info)
  (log/set-level! "telegrambot-lib" :info)
  (lp-app mybot))

