## 0.11.0 - 2024-04-28
* Added namespace `cloregram.dynamic` with dynamic Vars `*current-user` and `*from-message-id*`
* Added internationalisation logic and namespace `cloregram.texts`

## 0.10.3 - 2024-04-25
* Added forgotten file `src/cloregram/impl/users.clj` (facepalm...)
* Fixed bug about not restoring user's handler on callbackmquery processing

## 0.10.2 - 2024-04-24
* Added returning of `vuid` to `cloregram.validation.client` messages sending functions
* Changed [Datalevin](https://github.com/juji-io/datalevin) version to `0.9.5`

## 0.10.1.1 - 2024-04-18
* Fixed some mistakes in documentation

## 0.10.1 - 2024-04-17
* Moved internal implementation of working with Users from `cloregram.users` namespace

## 0.10.0 - 2024-04-17
* Added instructions to not put in cljdocs enything except public API
* It's a milestone. I just wanted beautiful version number :)

## 0.9.1 - 2024-04-16
* Changed datatabase to [Datalevin](https://github.com/juji-io/datalevin). Unexpectedly solved [<Issue #4>](https://github.com/algoflora/cloregram/issues/4)
* Major refactoring of functions and namespaces structure. [<Issue #11>](https://github.com/algoflora/cloregram/issues/11)
* Changed logging infrastructure to use [μ/log](https://github.com/BrunoBonacci/mulog?tab=readme-ov-file)
* Added fallback for message edit failure. [<Issue #15>](https://github.com/algoflora/cloregram/issues/15)
* Added limited media support. [<Issue #9>](https://github.com/algoflora/cloregram/issues/9)
* Added unnecessary callbacks cleaning logic. [<Issue #14>](https://github.com/algoflora/cloregram/issues/14)

## 0.9.0 - 2024-03-16
* Changed logging library. Using [taoensso.timbre](https://github.com/taoensso/timbre) from now [<Issue #8>](https://github.com/algoflora/cloregram/issues/8)
* Changed logging structures [<Issue #8>](https://github.com/algoflora/cloregram/issues/8)
* Minor refactorings

## 0.8.1 - 2024-03-08
* Changed `cloregram.test.infrastructure.client/click-btn` to throw Exception if no expected button

## 0.8.0 - 2024-03-07
* Added full `README.md` [<Issue #2>](https://github.com/algoflora/cloregram/issues/2)
* Added documentation for public main API functions
* Added documentation for public test infrastructure API functions
* Removed `get-user-info` from public test infrastructure API
* Fixed crash of `last-temp-message` if no temp messages

## 0.7.1 - 2024-03-04
* Moved `update-schema` and `load-data` to `cloregram.db`
* Changed `update-schema` and `load-data` to use `weavejester/resauce` library [<Issue #12>](https://github.com/algoflora/cloregram/issues/12)

## 0.7.0 - 2024-02-29
* Refactored testing infrastructure [<Issue #3>](https://github.com/algoflora/cloregram/issues/3)
