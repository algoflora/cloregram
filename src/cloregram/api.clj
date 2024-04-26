(ns cloregram.api
  (:require [cloregram.impl.api :as impl]
            [com.brunobonacci.mulog :as μ]))

(defn delete-message

  "Delete message with ID `mid` for `user`"

  {:added "0.11.0"}

  [user mid]
  (impl/delete-message user mid))

(defn send-message

  "Sends text message with content `text` and inline keyboard `kbd` to `user`.
  Possible `opts`:

  | option      | description | comment |
  |-------------|-------------|---------|
  | `:temp`     | Sends 'temporal' message that appears with notification under 'main' one. This message will have button to delete it in the end | |
  | `:markdown` | Messsage will use Markdown parse_mode | |
  | Long value  | Temporal Message ID you want to edit. It must be text message. `nil` as `kbd` value then means to leave keyboard layout unchanged | since 0.9.0 |" 

  {:changed "0.9.0"}

  [user text kbd & opts]
  (apply impl/prepare-and-send :message user text kbd opts))

(defn send-photo

  "Sends photo message with picture from java.io.File in `file` as a temporary message with caption `caption` and inline keyboard `kbd` to `user`.
  Possible `opts`:

  | option      | description |
  |-------------|-------------|
  | `:markdown` | Messsage will use Markdown parse_mode |
  | Long value  | Temporal Message ID you want to edit. It must be media message. `nil` as `kbd` value then means to leave keyboard layout unchanged |"
  
  {:added "0.9.1"}

  [user file caption kbd & opts]
  (apply impl/prepare-and-send :photo user {:file file :caption caption} kbd :temp opts))

(defn send-document

  "Sends java.io.File in `file` as a temporary message with caption `caption` and inline keyboard `kbd` to `user`.
  Possible `opts`:

  | option      | description | comment |
  |-------------|-------------|---------|
  | `:markdown` | Messsage will use Markdown parse_mode | |
  | Long value  | Temporal Message ID you want to edit. It must be media message. `nil` as `kbd` value then means to leave keyboard layout unchanged | since 0.9.0 |"
  
  {:added "0.4"
   :changed "0.9.0"}

  [user file caption kbd & opts]
  (apply impl/prepare-and-send :document user {:file file :caption caption} kbd :temp opts))


(defn send-invoice

  "Sends invoice as 'temporal' message with inline keyboard `kbd` to `user`. Keyboard will have payment button with `pay-text` in the beginning and button to delete it in the end.
  Description of `data` map (all keys required):
  
  | key               | description |
  |-------------------|-------------|
  | `:title`          | Product name, 1-32 characters
  | `:description`    | Product description, 1-255 characters
  | `:payload`        | Bot-defined invoice payload, 1-128 bytes. This will not be displayed to the user, use for your internal processes.
  | `:provider_token` | Payment provider token
  | `:currency`       | Three-letter ISO 4217 currency code
  | `:prices`         | Price breakdown, a JSON-serialized list of components (e.g. product price, tax, discount, delivery cost, delivery tax, bonus, etc.). Each component have to be map with keys `:label` (string) and `:amount` (integer price of the product in the smallest units of the currency)"

  {:added "0.5"}

  [user data pay-text kbd]
  (impl/prepare-and-send :invoice user data (vec (cons [{:text pay-text :pay true}] kbd)) :temp))

(defn get-file

  "Returns `java.io.File` or `nil` by `file-id`."

  {:added "0.9.1"}

  [file-id]
  (impl/get-file file-id))
