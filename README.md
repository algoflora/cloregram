# cloregram

Clojure/Datomic framework for making complex Telegram Bots/Applications

README.md is under development

## Installation

Download from http://example.com/FIXME.

## Usage

FIXME: explanation

## Options

FIXME: listing of options this app accepts.

## Examples

...

## Obtaining certificates
- In deploy folder create **ssl** folder: `mkdir ssl`
- Jump inside it: `cd ssl`
- Create certificate and private key. Fill in Country, State/Province, Locality and Organisation as you see fit. **CN** field is for IP-address or domain where bot is deploying. `openssl req -newkey rsa:2048 -sha256 -nodes -keyout private.key -x509 -days 365 -out cert.pem -subj "/C=LK/ST=Southern Province/L=Kathaluwa/O=Weedbreed/CN=127.0.0.1"`
- Obtain PKCS12 certificate from our keys: `openssl pkcs12 -export -in cert.pem -inkey private.key -out certificate.p12 -name "certificate"` It will ask you to come up with a password. It will be needed in next step.
- Create JKS keystore: `keytool -importkeystore -srckeystore certificate.p12 -srcstoretype pkcs12 -destkeystore keystore.jks` At first you will be asked about new keystore password. By default Cloregram using **cloregram.keystorepass**. Next step you have to enter password from previous step.
- Now you can delete **certificate.p12** and **private.key**: `rm certificate.p12 private.key`

Files **cert.pem** and **keystore.jks** as well as **ssl** folder can be diferent. Just then you need to specify in the config fields **:bot/server :options :keystore** (default to **ssl/keystore.jks**) and **:bot/instance :certificate** (default to **ssl/cert.pem**). As well you can set keystore password different from **cloregram.keystorepass** by specifying field **:bot/server :options :keystore-password**.
 
### Bugs

...

### Any Other Sections
### That You Think
### Might be Useful

## License

Copyright © 2023

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
