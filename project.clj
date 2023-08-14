(defproject magic-tray-bot "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}

  :dependencies [[org.clojure/clojure "1.11.1"]
                 [com.amperity/dialog "2.0.115"]
                 [nano-id "1.0.0"]

                 [telegrambot-lib "2.7.0"]
                 [cheshire "5.11.0"]
                 [clj-time "0.15.2"]
                 [org.clojure/tools.namespace "1.4.4"]

                 [com.datomic/peer "1.0.6735"]]

  :main ^:skip-aot magic-tray-bot.core

  :aliases {"reset-db" ["run" "-m" "magic-tray-bot.tasks.reset-db"]}

  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
