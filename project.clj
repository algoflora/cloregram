(defproject io.github.algoflora/cloregram "0.9.1.1"
  :description "Clojure framework for making complex Telegram Bots/Applications"
  ;:url "https://cloregram.io"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  
  :dependencies [[org.clojure/clojure "1.11.2"]
                 [integrant "0.8.1"]
                 [com.brunobonacci/mulog "0.9.0"]
                 [com.brunobonacci/mulog-zipkin "0.9.0"]
                 [com.brunobonacci/where "0.5.6"]
                 
                 [telegrambot-lib "2.13.0"]

                 [http-kit "2.7.0"]
                 [ring/ring-jetty-adapter "1.9.2"]

                 [nano-id "1.0.0"]
                 [cheshire "5.11.0"]
                 [resauce "0.2.0"]

                 [datalevin "0.9.4"]]

  :plugins [[lein-eftest "0.6.0"]]

  :main ^:skip-aot cloregram.core

  :target-path "target/%s"

  :jvm-opts ["--add-opens=java.base/java.nio=ALL-UNNAMED"
             "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED"]

  :profiles {:testing-stuff {:dependencies [[eftest "0.6.0"]
                                           [compojure "1.7.0"]
                                           [ring/ring-json "0.5.1"]
                                           [fivetonine/collage "0.3.0"]]
                            :resource-paths ["test/resources"]}
            :repl [:testing-stuff]
            :test [:testing-stuff]
            :uberjar {:aot :all
                      :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
