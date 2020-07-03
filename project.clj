(defproject cyberleague "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [javax.servlet/servlet-api "2.5"]
                 [http-kit  "2.1.16"]
                 [fogus/ring-edn "0.2.0"]
                 [de.ubercode.clostache/clostache "1.4.0"]
                 [org.clojure/data.json "0.2.5"]
                 [markdown-clj "0.9.54"]
                 [compojure "1.1.8"]
                 [me.raynes/fs "1.4.4"]
                 [environ "1.0.0"]
                 [com.datomic/datomic-free "0.9.4899"]
                 [org.clojure/math.numeric-tower "0.0.4"]
                 [org.clojure/google-closure-library "0.0-20151016-61277aea"]
                 [org.clojure/clojurescript "1.7.145"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [om "0.7.3"]
                 [prismatic/om-tools "0.3.6"]]

  :plugins [[jamesnvc/lein-lesscss "1.3.4"]
            [lein-environ "1.0.0"]]

  :source-paths ["src"]
  :lesscss-paths ["resources/less"]
  :lesscss-output-path "resources/public/css"

  :uberjar-name "cyberleague-standalone.jar"

  :main cyberleague.server.handler

  :profiles {:uberjar {:aot [cyberleague.server.handler]}

             :production
             {:cljsbuild {:builds
                          [{:id "cyberleague"
                            :source-paths ["src/cyberleague/client"]
                            :compiler {:pretty-print false
                                       :output-to "resources/public/js/out/cyberleague.min.js"
                                       :preamble ["react/react.min.js"]
                                       :externs ["react/externs/react.js"]
                                       :optimizations :advanced}}]}}

             :dev {:repl-options {:init-ns cyberleague.core}
                   :dependencies [[ring/ring-devel "1.3.0"]]
                   :plugins [[lein-cljsbuild "1.0.3"]
                             [quickie "0.2.5"]
                             [com.cemerick/clojurescript.test "0.3.1"]]
                   :test-paths ["test"]
                   :cljsbuild {:builds [{:id "cyberleague"
                                         :source-paths ["src/cyberleague/client"]
                                         :compiler {:output-to "resources/public/js/out/cyberleague.js"
                                                    :output-dir "resources/public/js/out"
                                                    :optimizations :none
                                                    :source-map true}}]}}})
