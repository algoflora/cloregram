## 0.9.1 - 
* Changed datatabase to [Datalevin](https://github.com/juji-io/datalevin). Unexpectedly solved [<Issue #4>](https://github.com/algoflora/cloregram/issues/4)

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
