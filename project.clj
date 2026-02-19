(defproject cyberleague "0.1.0-SNAPSHOT"

  :plugins [[io.bloomventures/omni "0.34.1"]]

  :omni-config cyberleague.omni-config/omni-config

  :source-paths ["src" "dev-src"]

  :test-paths ["test"]

  :dependencies [[com.hyperfiddle/rcf "20220926-202227"
                  :exclusions [org.clojure/clojure]]]
  :profiles {:server {:dependencies [; [org.clojure/clojure "1.12.4"] ;; from omni
                                     [io.bloomventures/omni "0.35.0"]
                                     [com.datomic/peer "1.0.7491"
                                      :exclusions
                                      [org.apache.httpcomponents/httpclient
                                       joda-time
                                       org.clojure/clojure
                                       commons-io]]

                                     [com.taoensso/tempel "1.1.0"]
                                     [tada "0.3.0"
                                      :exclusions [org.clojure/clojure
                                                   metosin/malli
                                                   org.clojure/spec.alpha]]

                                     ;; client
                                     [markdown-clj "1.10.4"
                                      :exclusions [org.clojure/clojure]]
                                     #_[reagent "0.10.0"] ;; from omni
                                     #_[org.clojure/clojurescript "1.10.764"] ;; from omni
                                     [cljsjs/codemirror "5.44.0-1"]
                                     [cljsjs/d3 "3.5.5-2"]
                                     #_[zprint "1.2.9"] ;; from omni->commons

                                     ;; coordinator
                                     [org.clojure/math.numeric-tower "0.0.4"
                                      :exclusions [org.clojure/clojure]]
                                     #_[org.clojure/data.json "1.0.0"]
                                     #_[org.babashka/sci "0.12.51"] ;; from omni->commons
                                     ;; if need macos or windows, see:
                                     ;; https://search.maven.org/search?q=g:com.eclipsesource.j2v8
                                     [com.eclipsesource.j2v8/j2v8_linux_x86_64 "4.8.0"]

                                     ;; registrar
                                     #_[metosin/malli "0.2.1"] ;; from omni->commoms
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
             :uberjar- {:aot :all
                        :dependencies
                        [[org.postgresql/postgresql "42.2.2"]]
                        :prep-tasks
                        [["omni" "compile"]
                         "compile"]}
             :uberjar [:server
                       :uberjar-]})
