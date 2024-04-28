# algoflora/cloregram

**[Clojure](https://clojure.org) and [Datalevin](https://github.com/juji-io/datalevin) framework for making complex Telegram Bots/Applications**

## Table of Contents

- [Idea](#idea)
- [Installation](#installation)
- [Usage](#usage)
  - [User](#user)
  - [API](#api)
  - [Keyboard](#keyboard)
  - [Handlers](#handlers)
  - [Payments](#payments)
  - [Interacting Datalevin database](#interacting-datalevin-database)
  - [Datalevin schema and initial data](#datalevin-schema-and-initial-data)
  - [Internationalisation](#internationalisation)
- [Testing](#testing)
- [Logging](#logging)
- [Configuration](#configuration)
- [Deploy](#deploy)
  - [Preparing system](#preparing-system)
  - [Starting your bot](#starting-your-bot)
  - [Obtaining certificates](#obtaining-certificates)
- [Further development and bugfixing](#further-development-and-bugfixing)
- [Roadmap](#roadmap)
- [License](#license)

## Idea
 
Cloregram has several main ideas:
1. **Main and temporal messages** - there is one main actual message that mostly reacts and interacting with user. For certain cases can be used such a "temporal" messages with button to delete them. Temporal messages appear with notification, when main one is changing smoothly.
2. **Stateless approach** - actually not completely stateless, but Cloregram does not save exact user state. Instead, each button points to unique Callback entity with user ref, function symbol and [EDN](https://github.com/edn-format/edn)-serialised arguments saved in [Datalevin](https://github.com/juji-io/datalevin). Only interaction with user input (text/media etc) needs to change user state. This approach allows to describe more robust and predictible bot behaviour.
3. **Virtual users testing** - Cloregram comes with ready-made integration testing framework. It mocks Telegram API server to allow developer describe users behaviour and test bot outcome in very convenient and flexible way.

## Installation

The simplest way to start project on Cloregram framework is to use [Leiningen](https://leiningen.org) template:
```
lein new algoflora/cloregram my-cloregram-bot
```

To check it use following commands:
```
cd my-cloregram-bot
lein eftest
```
*I am trying to support actual template version.*

## Usage

### User

Every user interacting with the bot is recorded in the database. User entity has following fields:

| Key | Description | Required? | Unique? |
|-------|-------|-------|------|
| `:user/username` | username of user in Telegram | | ☑️ (if exists) |
| `:user/id` | ID of user similar to chat_id | ☑️ | ☑️ |
| `:user/first-name` | first name of user in Telegram | ☑️ | |
| `:user/last-name` | last name of user in Telegram | | |
| `:user/language-code` | code of user's language in Telegram | ☑️ | |
| `:user/msg-id` | Id of **main** message. *Mostly for internal usage!* | ☑️ | |
| `:user/handler` | Current [handler](#handlers) for [Message](https://core.telegram.org/bots/api#message) with args. *Mostly for internal usage!* | ☑️ | |

In most cases current interacting with bot User is binded to `*current-user*` dynamic Var. Read further for more details and examples.

### API

Public API functions to interact with user are located in `cloregram.api` namespace. For now, support of many features like locations etc is missing. Support of media is very limited. Framework is still in active development.

Example usage:
```clojure
(cloregram.api/send-message *current-user* (format "*Hello!* Number is %d.\n\nWhat we will do?" n)
                                 [[["+" 'my-cloregram-bot.handlers/increment {:n n}]["-" 'my-cloregram-bot.handlers/decrement {:n n}]]]
                                 :markdown)
```

- `user` - the [user](#user) who will receive the message
- `(format <...>)` - text of the message
- vector of vectors of buttons - [keyboard](#keyboard) as collection of rows of buttons
- `:markdown` - option to use [MarkdownV2](https://core.telegram.org/bots/api#formatting-options) in message. 

All available functions and options look in API documentation. 

### Keyboard

Inline keyboard for mesage is presented as vector of button rows. Each row is a vector of buttons.

Each button can be presented as [map](https://core.telegram.org/bots/api#inlinekeyboardbutton). This is good for WebApp buttons, or buttons pointing on URLs.

In most cases we need button that performing certain action when clicked. For such buttons is convenient to use vector notation:
- first element is button text string
- second element is symbol of [handler function](#handlers) which will be called on click
- optional third element is arguments map for handler function

### Handlers

One of key points in Cloregram are handler functions.
Handler function takes a map of parameters. Always there will be key `:user` containing [User](#user) map.

All handling logic will have in scope dynamic Var `*current-user*` binded to [User](#user) from whom update was accepted. If current update carring callback query, then dynamic Var `*from-message-id*` wouldbe binded to ID of messasge where button with callback query was clicked. This is useful e.g. if you need to delete temporal message after some action from it. To use this vars you have to require `cloregram.dynamic` namespace: `(require '[cloregram.dynamic :refer :all])`.

If handler function is supposed to handle [Message](https://core.telegram.org/bots/api#message), then it will have [Message](https://core.telegram.org/bots/api#message) map in parameters map on key `:message`.

The main entry point is `my-cloregram-bot.handler/common`. It will be called on start or on any [Message](https://core.telegram.org/bots/api#message) input from user if this behaviour wasn't changed with calling `cloregram.users/set-handler`.

Following example of common handler will greet user by first name and repeat his text message:
```clojure
(ns my-cloregram-bot.handler
  (:require [cloregram.api :as api]
            [cloregram.dynamic :refer :all]))

(defn common
  [{:keys [user message]}]
  (api/send-message user
                    (format "Hi, %s!\n\nYou have said \"%s\", haven't you?"
                            (:user/first-name user)
                            (:text message))
                    []))
```

If handler function is suposed to handle [Callback Query](https://core.telegram.org/bots/api#callbackquery) (inline keyboard buttons clicks), then in parameter map will be `:user` key and other keys if any passed from button.

Look at extended example that will increment or decrement number value depending of button clicked:

```clojure
(ns my-cloregram-bot.handler
  (:require [cloregram.api :as api]
            [cloregram.dynamic :refer :all]))

(defn common
  [_]
  (api/send-message *current-user* (format "Hello, %s! Initial number is 0." (:user/first-name *current-user*))
                    [[["+" 'my-cloregram-bot.handler/increment {:n 0}]["-" 'my-cloregram-bot.handler/decrement {:n 0}]]]))

(defn increment
  [{:keys [n]}]
  (let [n (inc n)]
    (api/send-message *current-user* (format "Number was incremented: %d" n)
                      [[["+" 'my-cloregram-bot.handler/increment {:n n}]["-" 'my-cloregram-bot.handler/decrement {:n n}]]])))

(defn decrement
  [{:keys [n]}]
  (let [n (dec n)]
    (api/send-message *current-user* (format "Number was decremented: %d" n)
                    [[["+" 'my-cloregram-bot.handler/increment {:n n}]["-" 'my-cloregram-bot.handler/decrement {:n n}]]])))
```

Note that in this example any input except for button clicks will call `common` handler and reset number value to null! 

### Payments

To make user pay for something use API function `cloregram.api/send-invoice`. When user succesfully paid, payment handler is called. Payment handler have to be located in `my-cloregram-bot.handler/payment` function. This function take parameters map with keys [:user](#user) and [:payment](https://core.telegram.org/bots/api#successfulpayment). Use user data and `:invoice_payload` field in payment map to determine further behaviour.

### Interacting Datalevin database

Namespace `cloregram.database` provides functions for working with database:

| Call | Description | Comment |
|------|-------------|---------|
| `(cloregram.database/conn)` | Returns Datalevin connection | Look at [Datalevin Datalog store example](https://github.com/juji-io/datalevin/blob/master/README.md#use-as-a-datalog-store) for details
| `(cloregram.database/db)` | Returns database structure for queries, pulls etc | Look at [Datalevin Datalog store example](https://github.com/juji-io/datalevin/blob/master/README.md#use-as-a-datalog-store) for details

### Datalevin schema and initial data

Also `cloregram.db` namespace has two useful functions to keep the [schema](https://github.com/juji-io/datalevin/blob/master/README.md#use-as-a-datalog-store) up to date and to load initial data:

| Call | Description | Comment |
|------|-------------|---------|
| `(cloregram.db/update-schema)` | Wriring to database internal Cloregram schema ([User](#user) and Callback entities) and all entities from project's `resources/schema` folder. Schema entities data have to be located in **.edn** files. For conviency good to have different files for each entity. Nested folders are supported. | Take attention that this function is automatically launching every application startup. So kindly use `resources/schema` folder only for schema entities, but not for data. Otherwise data will be rewrited every startup even if it was changed by application. One more problem is that for now `update-schema` not removing entities attributes that are not in schema any more - ([Issue #6](https://github.com/algoflora/cloregram/issues/6)).
| `(cloregram.db/load-data)` | Writing to database data from project's `resources/data` folder. Data have to be in **.edn** files. For conviency good to have different files for each entity. Nested folders are supported. | Take attention that this function is not called anywhere in code except `cloregram.test.fixtures/load-initial-data` test fixture. So you have to manage manual calling of `load-data` after project startup at first launch in production. Don't forget remove this call later or all involved data will be always overwritten from scratch. This behaviour expected to be changed in [Issue #6](https://github.com/algoflora/cloregram/issues/6).

### Internationalisation

For internationalisation can be used built-in `cloregram.texts` namespace. At first you have to create folder 'texts' in project's resource path. In that folder you an put any number of **.edn** files, each representing clojure nested map. These maps will be deeply merged on startup. On the deepest level all values must be a map with language codes as keys and corresponding strings as values.

For example you have file `resources/texts/main.edn`:

```clojure
{:main {:en "Select action:"
        :fr "Sélectionnez l'action:"}
 :greeting {:en "Hello, %s!"
            :fr "Bonjour, %s!"}}
```

as well as `resources/texts/buttons.edn`:

```clojure
{:buttons {:greet-en {:en "Greet in English"
                      :fr "Saluer en anglais"}
		   :greet-fr {:en "Greet in French"
	                  :fr "Saluer en français"}
           :back {:en "Back"
                  :fr "Dos"}}}
```

Now you can use `cloregram.texts/txt` and `cloregram.texts/txti` functions. Difference is that `txti` function accepts explicit language code as fifst argument, while `txt` function uses `:language-code` field of `*current-user*`. Both functions accept various arguments possibly used in string formatting.

```clojure
(ns my-cloregram-bot.handler
  (:require [cloregram.api :as api]
            [cloregram.dynamic :refer :all]
            [cloregram.texts :refer [txt txti]]))
			
(defn common
  [_]
  (api/send-message *current-user* 
                    (txt :main) 
					[[[(txt [:buttons :greet-en]) 'my-cloregram-bot.handler/greet {:lang :en}]]
                     [[(txt [:buttons :greet-fr]) 'my-cloregram-bot.handler/greet {:lang :fr}]]]))

(defn greet
  [{:keys [lang]}]
  (api/send-message *current-user*
                    (txti lang :greeting (:user/first-name *current-user*))
					[[[(txt [:buttons :back]) 'my-cloregram.handler/common]]]))
```

## Testing

Cloregram has powerful integration testing suite in `cloregram.validation` namespace. Main idea is to simulate behaviour of users with virtual ones and check bot output. This approach in the ideal case allows to test all scenarios, involving any number of virtual users.

Framework has useful fixtures to prepare testing environment and load [initial data](#schema-and-initial-data):
```clojure
(ns my-cloregram-bot.core-test
  (:require [clojure.test :refer :all]
            [cloregram.validation.fixtures :as fix]))

(use-fixtures :once fix/use-test-environment fix/load-initial-data)
```

The `cloregram.validation.users` namespace is responsible for working with virtual users:
```clojure
(require '[cloregram.validation.users :as u])

(u/add :user-1) ; creates virtual user with username "user-1"
(u/add :user-2) ; creates virtual user with username "user-2"

(u/main-message :user-1) ; => nil
(u/last-temp-mesage :user-2) ; => nil
(u/count-temp-messages :user-2) ; => 0
```

The `cloregram.validation.client` namespace contains functions for interaction with bot by virtual users:
```clojure
(require '[cloregram.validation.client :as c])

(c/send-text :user-1 "Hello, bot!") ; sends to bot the text message "Hello, bot!" by virtual user "user-1"
```
Also in this namespace are functions: 
- `click-btn` to simulate clicking button in incoming message
- `pay-invoice` to simulate clicking Pay button in incoming invoice
- `send-photo` to send to bot image from resources
- `send-message` to send more generic messages, not for common use cases

The `cloregram.test.infrastructure.inspector` namespace contains functions to check contents of incoming messages:
```clojure
(require '[cloregram.test.infrastructure.inspector :as i])

(def msg ....) ; message structure

(i/check-text msg "Hello from bot!") ; asserts message's text
(i/check-btns msg [["To Main Menu"]] ; asserts keyboard layout
(i/check-document msg "Caption" contents) ; asserts incoming document caption and contents
(i/check-photo msg "Photo caption" resource) ; asserts incoming photo caption and equality of image to certain resource
(i/check-invoice msg expected-invoice-data) ; asserts incoming invoice
```

Common test workflow can be like following:
```clojure
(ns my-cloregram-bot.core-test
  (:require [clojure.test :refer :all]
            [cloregram.validation.fixtures :as fix]
            [cloregram.validation.users :as u]
            [cloregram.validation.client :as c]
            [cloregram.validation.inspector :as i]))

(use-fixtures :once fix/use-test-environment)

(deftest core-test
  (testing "Core Test"
    (u/add :testuser-1)
    
    (c/send-text :testuser-1 "/start")
    (-> (u/main-message :testuser-1)
        (i/check-text "Hello, testuser-1! Initial number is 0.")
        (i/check-btns [["+" "-"]])
        (c/click-btn :testuser-1 "+"))
    (-> (u/main-message :testuser-1)
        (i/check-text "Number was incremented: 1")
        (i/check-btns [["+" "-"]])
        (c/click-btn :testuser-1 "+"))
    (-> (u/main-message :testuser-1)
        (i/check-text "Number was incremented: 2")
        (i/check-btns [["+" "-"]])
        (c/click-btn :testuser-1 "-"))
    (-> (u/main-message :testuser-1)
        (i/check-text "Number was decremented: 1"))))
```

Cloregram is already configurated to use for testing purposes [weavejester/eftest](https://github.com/weavejester/eftest) [Leiningen](https://leiningen.org) plugin.

## Logging

For logging Cloregram using [μ/log](https://github.com/BrunoBonacci/mulog) library.

Out of the box it write all logs to **logs/logs.mulog** file.
Also: 
- **logs/last.mulog** - all logs of last run. Cleans at startup.
- **logs/events.mulog** - logs of last run events. No traces. Cleans at startup.
- **logs/errors.mulog** - errors of last run. Cleans on startup.
- **logs/publishers-errors.mulog** - errors in μ/log publishers. Cleans at startup.

## Configuration

You have to create in `resources` folder file `config.prod.edn` for production deploy as well as `confid.dev.edn` and `config.test.edn` if needed. These files will be used with corresponding [Leiningen](https://leiningen.org) profiles. More configs and profiles can be added manually. To reference use file `resouces/config.example.edn` and following table:

| Key          | Description | Default | Comment | 
|--------------|-------------|---------|---------|
| `:bot/token` | Bot's token obtained from BotFather | "0000000000:XXX..." | Default value works in tests |
| `:bot/ip` | Ip address of Bot application | "0.0.0.0" | |
| `:bot/port` | Port Bot application to listen | 8443 | |
| `:bot/https?` | Will bot use HTTPS? | `false` | In production this value must be `true` |
| `:bot/admins` | List of administrators usernames | | Legacy option. Will be revised as part of [Issue #5](https://github.com/algoflora/cloregram/issues/5) |
| `:bot/api-url` | Telegram API URL if not default is used | `nil` | `nil` means common Telegram Bot API. Different value is used in tests and can be useful if you are deployed [local bot API server](https://core.telegram.org/bots/api#using-a-local-bot-api-server) |
| `:bot/server -> :options -> :keystore` | Keystore path for SSL | "./ssl/keystore.jks" | Look [Obtaining certificates](#obtaining-certificates) for details |
| `:bot/server -> :options -> :keystore-password` | Password for SSL keystore | "cloregram.keystorepass" | Look [Obtaining certificates](#obtaining-certificates) for details |
| `:bot/instance -> :certificate` | Path to PEM certificate | "./ssl/cert.pem" | Look [Obtaining certificates](#obtaining-certificates) for details |
| `:db/connection -> :uri` | Datalevin database URI | | In tests `/tmp/cloregram-datalevin` is used. |
| `:db/connection -> :clear?` | Clear the database on startup? | `false` | In tests value is `true` to clear temporal database for each run. |
| `:project/config` | Map of project specific config | `{}` | Values from this map could be accessed with hekp of function `(cloregram.system.state/config :key :nested-key ...)` |

## Deploy

If you (`my-username`) want to deploy the bot (`my-cloregram-bot`) in home directory on Ubuntu server on address `127.1.2.3`, use following instructions:

### Preparing system

On your server install required JAVA package:
```
sudo apt update && sudo apt install -y default-jre-headless
```

### Starting your bot

Then create there folder structure for project:
```
cd ~
mkdir my-cloregram-bot
cd my-cloregram-bot
mkdir logs
```

On local machine (or in CI script) in project sources root folder run (pay attention to your current project version):
```
lein uberjar
rsync target/uberjar/my-cloregram-bot-0.1.0-standalone.jar my-username@127.1.2.3:/home/my-username/my-cloregram-bot/my-cloregram-bot-standalone.jar
```

On server in project folder run the bot:
```
java -jar my-cloregram-bot-standalone.jar
```
You can use cli option `-XX:-OmitStackTraceInFastThrow` for better errors display, but this option may slightly reduce performance.

**Or you can start the bot as systemd service**

Use editor to create service file `/etc/systemd/system/my-cloregram-bot.service`:
```
[Unit]
Description=My Cloregram Bot Service
After=network.target

[Service]
User=my-username
ExecStart=java -XX:-OmitStackTraceInFastThrow -jar /home/my-username/my-cloregram-bot/my-cloregram-bot-standalone.jar
WorkingDirectory=/home/my-username/my-cloregram-bot
Restart=always

[Install]
WantedBy=multi-user.target
```

Then start service, enable it on startup and check health:
```
sudo systemctl start my-cloregram-bot.service
sudo systemctl enable my-cloregram-bot.service
sudo systemctl status my-cloregram-bot.service
```

In cause of updated .jar file restart service:
```
sudo systemctl restart my-cloregram-bot.service
```

### Obtaining certificates

To ensure the correct operation of bot webhooks, you must prepare your SSL certificates::

- In deploy folder create **ssl** folder: `mkdir ssl`
- Jump inside it: `cd ssl`
- Create certificate and private key. Fill in Country, State/Province, Locality and Organisation as you see fit. **CN** field is for IP-address or domain where bot is deploying. `openssl req -newkey rsa:2048 -sha256 -nodes -keyout private.key -x509 -days 365 -out cert.pem -subj "/C=LK/ST=Southern Province/L=Kathaluwa/O=Algoflora/CN=127.1.2.3"`
- Obtain PKCS12 certificate from our keys: `openssl pkcs12 -export -in cert.pem -inkey private.key -out certificate.p12 -name "certificate"` It will ask you to come up with a password. It will be needed in next step
- Create JKS keystore: `keytool -importkeystore -srckeystore certificate.p12 -srcstoretype pkcs12 -destkeystore keystore.jks` At first you will be asked about new keystore password. By default Cloregram using **cloregram.keystorepass**. Next step you have to enter password from previous step
- Now you can delete **certificate.p12** and **private.key**: `rm certificate.p12 private.key`

Files **cert.pem** and **keystore.jks** as well as **ssl** folder can be different. Just then you need to specify in the config fields **:bot/server :options :keystore** (default to **ssl/keystore.jks**) and **:bot/instance :certificate** (default to **ssl/cert.pem**). As well you can set keystore password different from **cloregram.keystorepass** by specifying field **:bot/server :options :keystore-password**.

## Further development and bugfixing

The process of developing the framework depends on the needs of specific projects and/or the author's inspiration. Take a look on [Issues](https://github.com/algoflora/cloregram/issues) page and feel free to suggest something there.

## Roadmap

* write spec and unit tests
* add opportunity to select database in config (Datalevin/Datomic)
* enhance and expand media support
* add flexible logging configuration
* enhance payment support

## License

Copyright © 2023-2024

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
