(defproject cyberleague "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [javax.servlet/servlet-api "2.5"]
                 [pog "1.0.0"]
                 [http-kit  "2.1.16"]
                 [fogus/ring-edn "0.2.0"]
                 [de.ubercode.clostache/clostache "1.4.0"]
                 [org.clojure/data.json "0.2.5"]
                 [markdown-clj "0.9.54"]
                 [compojure "1.1.8"]
                 [me.raynes/fs "1.4.4"]
                 [environ "1.0.0"]
                 ; cljs-related:
                 [org.clojure/clojurescript "1.7.145"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [om "0.7.3"]
                 [prismatic/om-tools "0.3.6"]]

  :plugins [[jamesnvc/lein-lesscss "1.3.4"]
            [lein-environ "1.0.0"]]

  :source-paths ["src/clj" "src/cljs"]
  :lesscss-paths ["resources/less"]
  :lesscss-output-path "resources/public/css"

  :uberjar-name "cyberleague-standalone.jar"

  :main cyberleague.handler

  :profiles {:uberjar {:aot [cyberleague.handler]}

             :production
             {:cljsbuild {:builds
                          [{:id "cyberleague"
                            :source-paths ["src/cljs"]
                            :compiler {:pretty-print false
                                       :output-to "resources/public/js/out/cyberleague.min.js"
                                       :preamble ["react/react.min.js"]
                                       :externs ["react/externs/react.js"]
                                       :optimizations :advanced}}]}}

             :dev {:repl-options {:init-ns cyberleague.handler}
                   :dependencies [[org.clojure/tools.reader "0.8.9"]
                                  [ring/ring-devel "1.3.0"]]
                   :plugins [[lein-cljsbuild "1.0.3"]
                             [quickie "0.2.5"]]
                   :test-paths ["test/clj"]
                   :cljsbuild {:builds [{:id "cyberleague"
                                         :source-paths ["src/cljs"]
                                         :compiler {:output-to "resources/public/js/out/cyberleague.js"
                                                    :output-dir "resources/public/js/out"
                                                    :optimizations :none
                                                    :source-map true}}]}}})

