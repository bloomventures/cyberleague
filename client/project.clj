(defproject cyberleague "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [javax.servlet/servlet-api "2.5"]
                 [com.datomic/datomic-pro "0.9.4899"]
                 [http-kit  "2.1.16"]
                 [fogus/ring-edn "0.2.0"]
                 [org.clojure/data.json "0.2.5"]
                 [markdown-clj "0.9.54"]
                 [compojure "1.1.8"]
                 [org.clojure/clojurescript "0.0-2280"]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]
                 [om "0.7.1"]]

  :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                   :username "james@leanpixel.com"
                                   :password "eb6ce9b2-92f7-4164-86c3-49e60732bba9"}}

  :plugins [[jamesnvc/lein-lesscss "1.3.4"]]

  :source-paths ["src/clj" "src/cljs"]
  :lesscss-paths ["resources/less"]
  :lesscss-output-path "resources/public/css"

  :uberjar-name "cyberleague-standalone.jar"

  :main cyberleague.handler

  :profiles {:uberjar {:aot :all}
             :dev {:repl-options {:init-ns cyberleague.handler}
                   :dependencies [[org.clojure/tools.reader "0.8.9"]]
                   :plugins [[lein-cljsbuild "1.0.3"]
                             [quickie "0.2.5"]]
                   :test-paths ["test/clj"]
                   :cljsbuild {:builds [{:id "cyberleague"
                                         :source-paths ["src/cljs" ]
                                         :compiler {:output-to "resources/public/js/out/cyberleague.js"
                                                    :output-dir "resources/public/js/out"
                                                    :optimizations :none
                                                    :source-map true}}]}}})

