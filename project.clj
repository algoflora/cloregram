(defproject magic-tray-bot "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}

  :java-cmd "/opt/homebrew/opt/openjdk/bin/java"
  
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [integrant "0.8.1"]
                 [com.amperity/dialog "2.0.115"]

                 [telegrambot-lib "2.9.0"]
                 [http-kit "2.7.0"]
                 [nano-id "1.0.0"]
                 [cheshire "5.11.0"]
                 [clj-time "0.15.2"]
                                  
                 [com.datomic/peer "1.0.6735"]]

  :plugins [[lein-eftest "0.6.0"]]

  :main ^:skip-aot magic-tray-bot.core

  :aliases {"db-reset" ["run" "-m" "magic-tray-bot.tasks.reset-db"]
            "sch-up" ["run" "-m" "magic-tray-bot.tasks.update-schema"]}

  :target-path "target/%s"
  :profiles {:test {:dependencies [[eftest "0.6.0"]]}
             :uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
