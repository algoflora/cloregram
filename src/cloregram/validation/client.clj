(ns cloregram.validation.client
  (:require [cloregram.impl.validation.client :as impl]
            [com.brunobonacci.mulog :as μ]))

(defn send-message

  "Simulate sending raw message represented in `data` by virtual user with username `vuid`. In most cases you don't need this function. Use it only if you definitely know what you are doing. Optionaly use `:silent` option to not save message in virtual user's state. Returns `vuid`."

  {:changed "0.10.2"}

  [vuid data & opts]
  (μ/trace ::simulate-raw-message
           [:simulate-raw-message/vuid vuid
            :simulate-raw-message/data data
            :simulate-raw-message/options opts]
           (apply impl/send-message vuid data opts)))

(defn send-text

  "Simulate sending `text` by virtual user with username `vuid`. Optionaly `entities` array can be provided for formatting message. Returns `vuid`."

  {:changed "0.10.2"}

  ([vuid text] (send-text vuid text []))
  ([vuid text entities]
   (μ/trace ::simulate-text-message
            [:simulate-text-message/vuid vuid
             :simulate-text-message/text text]
            (impl/send-message vuid {:text text :entities entities}))))

(defn send-photo

  "Simulate sending photo with optional `caption` from resource `path` by virtual user with username `vuid`. Optionaly `entities` array can be provided for formatting caption. Returns `vuid`."

  {:added "0.9.1"
   :changed "0.10.2"}

  ([vuid path] (send-photo vuid nil path))
  ([vuid caption path] (send-photo vuid caption [] path))
  ([vuid caption entities path]
   (μ/trace ::simulate-photo-message
            [:simulate-photo-message/vuid vuid
             :simulate-photo-message/path path
             :simulate-photo-message/caption caption]
            (impl/send-photo vuid caption entities path))))

(defn click-btn
  
  "Simulate clicking button in `row` and `col` or with text `btn` in message `msg` by virtual user with username `vuid`. Exception would be thrown if there is no expected button. Returns `msg`"

  {:added "0.9.1"}

  ([msg vuid row col]
   (μ/trace ::simulate-button-click
            [:simulate-button-click/message msg
             :simulate-button-click/vuid vuid
             :simulate-button-click/button-row row
             :simulate-button-click/button-column col]
            (impl/click-btn msg vuid row col)))
  ([msg vuid btn-text]
   (μ/trace ::simulate-button-click
            [:simulate-button-click/message msg
             :simulate-button-click/vuid vuid
             :simulate-button-click/button-text btn-text]
            (impl/click-btn msg vuid btn-text))))

(defn pay-invoice

  "Simulate payment for invoice from message `msg` by virtual user with username `vuid`. Returns `msg`"

  {:added "0.9.1"}

  [msg vuid]
  (μ/trace ::simulate-invoice-payment
           [:simulate-invoice-payment/message msg
            :simulate-invoice-payment/vuid vuid]
           (impl/pay-invoice msg vuid)))
