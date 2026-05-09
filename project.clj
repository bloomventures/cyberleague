(defproject cyberleague "0.1.0-SNAPSHOT"

  :plugins [[io.bloomventures/omni "0.36.2"]
            [io.taylorwood/lein-native-image "0.3.1"]]

  :omni-config cyberleague.omni-config/omni-config

  :jvm-opts ["--enable-native-access=ALL-UNNAMED"]

  :test-paths ["test"]

  :dependencies [[com.hyperfiddle/rcf "20220926-202227"
                  :exclusions [org.clojure/clojure]]]
  :profiles {:common {:dependencies [[com.taoensso/telemere "1.2.1"]
                                     [org.clj-commons/digest "1.4.100"]
                                     [com.cognitect/transit-clj "1.1.357"]]}
             :dev {:source-paths ["dev-src"]
                   ;; for cli building
                   :plugins [[lein-binplus "0.6.8"]]
                   :repl-options {:init-ns user}}
             ;; leingen complains about mixing keywords and maps
             ;; hence the :*foo profiles

             ;; EVALUATOR
             :*evaluator {:source-paths ["evaluator-src"]
                          :dependencies [[org.clojure/clojure "1.12.4"]
                                         [http-kit "2.8.0"]
                                         [io.bloomventures/commons "0.17.1"]
                                         [com.rpl/specter "1.1.6"]
                                         [com.dylibso.chicory/runtime "1.7.5"]
                                         [com.dylibso.chicory/wasi "1.7.5"]
                                         [ring/ring-defaults "0.7.0"]
                                         #_[metosin/muuntaja "0.6.11"]
                                         #_[org.clojure/data.json "2.5.2"]

                                         [mvxcvi/clj-cbor "1.1.1"]

                                         [tada "0.3.0"]
                                         [buddy/buddy-sign "3.6.1-359"]

                                         ;; for dev evaluator (non-firecracker)
                                         [org.babashka/sci "0.12.51"]]
                          :main cyberleague.evaluator.core}
             :evaluator [:common :*evaluator]
             :*uberjar-evaluator {:aot [cyberleague.evaluator.core]}
             :uberjar-evaluator [:evaluator :*uberjar-evaluator]
             :test {:source-paths ["test"]}
             ;; SERVER
             :*server {:main cyberleague.core
                       :source-paths ["src"]
                       :dependencies [[org.clojure/clojure "1.12.4"] ;; from omni
                                      [org.clojure/clojurescript "1.12.134"]
                                      [io.bloomventures/omni "0.36.2"]
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

                                      [com.github.rafd/dat "0.0.1-20260509-0"]

                                      ;; coordinator
                                      [org.clojure/math.numeric-tower "0.0.4"
                                       :exclusions [org.clojure/clojure]]

                                      ;; bot weight
                                      [com.aayushatharva.brotli4j/brotli4j "1.20.0"]
                                      #_[org.clojure/data.json "1.0.0"]

                                      ;; registrar
                                      #_[metosin/malli "0.20.1"] ;; from omni->commoms
                                      [org.babashka/sci "0.12.51"]
                                      ] }
             :server [:common :*server]
             :*uberjar-server {:aot [cyberleague.core]
                               :source-paths ["src"]
                               :dependencies
                               [[org.postgresql/postgresql "42.2.2"]]
                               :prep-tasks
                               [["omni" "compile"]
                                "compile"]}
             ;; lein with-profile uberjar-server uberjar
             :uberjar-server [:server :*uberjar-server]

             ;; CLI
             :*cli {:target-path "cli-target"
                    :source-paths ["cli-src"]
                    :resource-paths ^:replace []
                    :main         cyberleague.cli.core
                    :dependencies [[cli-matic "0.5.4"]
                                   [zprint "1.3.0"]
                                   [metosin/malli "0.20.1"]
                                   [org.clojure/clojure "1.11.4"]
                                   ;; bot weight
                                   [org.tukaani/xz "1.12"]
                                   [cheshire "5.13.0"]

                                   [http-kit "2.8.0"]
                                   [com.nextjournal/beholder "1.0.3"]]
                    :repl-options {:init-ns cyberleague.cli.core}
                    }
             :cli [:common :*cli]
             :*bin-cli {:aot [cyberleague.cli.core]
                        :bin {:name "cyberleague"}}
             ;; lein with-profile bin-cli bin
             ;; creates target/cyberleague which is an uberjar that can be self-executed
             ;; ie ./cyberleague instead of java -jar cyberleague.jar
             ;; details in: https://github.com/BrunoBonacci/lein-binplus
             :bin-cli [:cli :*bin-cli]
             ;; GRAALVM_HOME="/" lein with-profile native-image native-image
             :*native-image {:source-paths ^:replace ["cli-src"]
                             :jvm-opts ["-Dclojure.compiler.direct-linking=true"]
                             :dependencies [[com.github.clj-easy/graal-build-time "1.0.5"]]
                             :native-image {:name "cyber"
                                            :opts ["--features=clj_easy.graal_build_time.InitClojureClasses"
                                                   "--enable-url-protocols=http,https"
                                                   "--verbose"]}}
             :native-image [:cli :*native-image]
             })
