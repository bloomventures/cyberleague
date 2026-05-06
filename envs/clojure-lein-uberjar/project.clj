(defproject bot "0.0.1-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.12.4"]
                 [org.clojure/data.json "2.5.2"]]
  :main bot.core
  :profiles {:uberjar {:aot :all}})
