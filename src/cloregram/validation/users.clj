(ns cloregram.validation.users
  (:require [com.brunobonacci.mulog :as μ]
            [cloregram.impl.validation.users :as impl]))

(defn add
  
  "Creates virtual user with username and first-name `vuid`, language-code 'en' and empty mesages storage. Writes this user into test infrastructure virtual users state storage with key `vuid`."

  {:changed "0.9.1"}

  [vuid]
  (impl/add-v-user vuid))

(defn get-v-user-by-vuid
  
  "Returns virtual user structure by key `vuid` from validation suit virtual users state storage"

  [vuid]
  (impl/get-v-user-by- :username (name vuid)))

(defn main-message

  "Returns main message of virtual user with key `vuid` from test infrastructure virtual users state storage. If virtual user is not awaiting response, then message structure or nil is returned immidiately. If virtual user is awaiting response, then function will wait until response or `timeout` milliseconds (default: 2000). If awaiting of response not finished while timeout passed, Exception will be throwed."

  {:added "0.9.1"}

  ([vuid] (main-message vuid 2000))
  ([vuid timeout]
   (impl/get-response-or-current vuid 'main-message timeout)))

(defn last-temp-message

  "Returns last temporal message of virtual user with key `vuid` from test infrastructure virtual users state storage. If virtual user is not awaiting response, then temporal message structure or nil is returned immidiately. If virtual user is awaiting response, then function will wait until response or `timeout` milliseconds (default 2000). If awaiting of response not finished while timeout passed, Exception will be throwed."

  {:added "0.9.1"}

  ([vuid] (last-temp-message vuid 2000))
  ([vuid timeout]
   (impl/get-response-or-current vuid 'last-temp-message timeout)))

(defn count-temp-messages

  "Returns count of temporal messages of virtual user with key `vuid` from test infrastructure virtual users state storage. If virtual user is not awaiting response, then temporal messages count is returned immidiately. If virtual user is awaiting response, then function will wait until response or `timeout` milliseconds (default 2000). If awaiting of response not finished while timeout passed, Exception will be throwed."

  {:added "0.9.1"}

  ([vuid] (count-temp-messages vuid 2000))
  ([vuid timeout]
   (impl/get-response-or-current vuid 'count-temp-messages timeout)))




