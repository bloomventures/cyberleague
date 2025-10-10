(defproject cyberleague "0.1.0-SNAPSHOT"

  :plugins [[io.bloomventures/omni "0.32.2"]]

  :omni-config cyberleague.omni-config/omni-config

  :source-paths ["src"]

  :test-paths ["test"]

  :dependencies [[com.hyperfiddle/rcf "20220926-202227"]]
  :profiles {:server {:dependencies [[io.bloomventures/omni "0.32.2"]
                                     [com.datomic/peer "1.0.6733"
                                      :exclusions
                                      [com.google.guava/guava
                                       commons-codec
                                       org.clojure/tools.cli
                                       org.slf4j/slf4j-nop
                                       org.slf4j/jcl-over-slf4j
                                       org.slf4j/slf4j-api]]

                                     [tada "0.3.0"]

                                     ;; client
                                     [markdown-clj "1.10.4"]
                                     #_[reagent "0.10.0"]
                                     #_[org.clojure/clojurescript "1.10.764"]
                                     [cljsjs/codemirror "5.44.0-1"]
                                     [cljsjs/d3 "3.5.5-2"]
                                     [zprint "1.2.9"]

                                     ;; coordinator
                                     [org.clojure/math.numeric-tower "0.0.4"]
                                     #_[org.clojure/data.json "1.0.0"]
                                     [borkdude/edamame "1.4.26"]
                                     [org.babashka/sci "0.8.43"
                                      :exclusions [org.clojure/tools.reader]]
                                     ;; if need macos or windows, see:
                                     ;; https://search.maven.org/search?q=g:com.eclipsesource.j2v8
                                     [com.eclipsesource.j2v8/j2v8_linux_x86_64 "4.8.0"]

                                     ;; registrar
                                     ;; using malli from tada
                                     ;; [metosin/malli "0.2.1"]
                                     ]

                      :main cyberleague.core
                      :repl-options {:init-ns cyberleague.core}}
             :cli     {:main         cyberleague.cli.core
                       :dependencies [[org.clojure/tools.cli "1.1.230"]
                                      [com.cognitect/transit-clj "1.0.324"]
                                      [org.clojure/clojure "1.11.4"]
                                      [http-kit "2.8.0"]
                                      [com.nextjournal/beholder "1.0.2"]]
                       :repl-options {:init-ns cyberleague.cli.core}}
             :uberjar {:aot :all
                       :dependencies
                       [[org.postgresql/postgresql "42.2.2"]]
                       :prep-tasks
                       [["omni" "compile"]
                        "compile"]}})
