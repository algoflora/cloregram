(defproject cloregram "0.6.6.2"
  :description "Clojure/Datomic framework for making complex Telegram Bots/Applications"
  :url "https://cloregram.io"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [integrant "0.8.1"]
                 [com.amperity/dialog "2.0.115"]

                 [telegrambot-lib "2.12.0"]

                 [ring/ring-jetty-adapter "1.9.2"]

                 [nano-id "1.0.0"]
                 [cheshire "5.11.0"]
                 
                 [com.datomic/peer "1.0.7075"]]
  
  :plugins [[lein-eftest "0.6.0"]]

  :main ^:skip-aot cloregram.core
  
  :target-path "target/%s"
  :profiles {:test {:dependencies [[eftest "0.6.0"]
                                   [http-kit "2.7.0"]
                                   [compojure "1.7.0"]
                                   [ring/ring-json "0.5.1"]]}
             :uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})


