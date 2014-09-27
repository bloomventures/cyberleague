(defproject cyberleague "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [javax.servlet/servlet-api "2.5"]
                 [http-kit  "2.1.16"]
                 [compojure "1.1.8"]
                 [org.clojure/clojurescript "0.0-2280"]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]
                 [om "0.7.1"]]

  :plugins [[jamesnvc/lein-lesscss "1.3.4"]]

  :source-paths ["src/clj" "src/cljs"]
  :lesscss-paths ["resources/less"]
  :lesscss-output-path "resources/public/css"

  :uberjar-name "cyberleague-standalone.jar"

  :main cyberleague.handler

  :profiles {:uberjar {:aot :all}
             :dev {:repl-options {:init-ns cyberleague.handler}
                   :plugins [[lein-cljsbuild "1.0.3"]]
                   :cljsbuild {:builds [{:id "cyberleague"
                                         :source-paths ["src/cljs" ]
                                         :compiler {:output-to "resources/public/js/out/cyberleague.js"
                                                    :output-dir "resources/public/js/out"
                                                    :optimizations :none
                                                    :source-map true}}]}}})

