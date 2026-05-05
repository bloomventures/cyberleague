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
                   :repl-options {:init-ns user}}
             ;; leingen complains about mixing keywords and maps
             ;; hence the :*foo profiles
             :*evaluator {:source-paths ["evaluator-src"]
                          :dependencies [[org.clojure/clojure "1.12.4"]
                                         [http-kit "2.8.0"]
                                         [io.bloomventures/commons "0.17.1"]
                                         [com.rpl/specter "1.1.6"]
                                         [com.dylibso.chicory/runtime "1.7.5"]
                                         [com.dylibso.chicory/wasi "1.7.5"]
                                         #_[ring/ring-defaults "0.7.0"]
                                         #_[metosin/muuntaja "0.6.11"]
                                         #_[org.clojure/data.json "2.5.2"]

                                         [mvxcvi/clj-cbor "1.1.1"]

                                         [tada "0.3.0"]
                                         [buddy/buddy-sign "3.6.1-359"]

                                         ;; for dev evaluator (non-firecracker)
                                         [org.babashka/sci "0.12.51"]]
                          :main cyberleague.evaluator.core}
             :evaluator [:common :*evaluator]
             :*server {:main cyberleague.core
                       :source-paths ["src"]
                       :dependencies [; [org.clojure/clojure "1.12.4"] ;; from omni
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
                                      #_[zprint "1.2.9"] ;; from omni->commons

                                      [com.github.rafd/dat "0.0.1-20260505-0"]

                                      ;; coordinator
                                      [org.clojure/math.numeric-tower "0.0.4"
                                       :exclusions [org.clojure/clojure]]

                                      ;; bot weight
                                      [com.aayushatharva.brotli4j/brotli4j "1.20.0"]
                                      #_[org.clojure/data.json "1.0.0"]

                                      ;; registrar
                                      #_[metosin/malli "0.2.1"] ;; from omni->commoms
                                      ] }
             :server [:common :*server]
             :*cli {:source-paths ["cli-src"]
                    :resource-paths ^:replace []
                    :main         cyberleague.cli.core
                    :dependencies [[cli-matic "0.5.4"]
                                   [zprint "1.3.0"]
                                   [metosin/malli "0.20.1"]
                                   [org.clojure/clojure "1.11.4"]
                                   ;; bot weight
                                   [org.tukaani/xz "1.12"]

                                   [http-kit "2.8.0"]
                                   [com.nextjournal/beholder "1.0.3"]]
                    :repl-options {:init-ns cyberleague.cli.core}}
             :cli [:common :*cli]
             ;; GRAALVM_HOME="/" lein with-profile native-image native-image
             :*native-image {:source-paths ^:replace ["cli-src"]
                             :jvm-opts ["-Dclojure.compiler.direct-linking=true"]
                             :dependencies [[com.github.clj-easy/graal-build-time "1.0.5"]]
                             :native-image {:name "cyber"
                                            :opts ["--features=clj_easy.graal_build_time.InitClojureClasses"
                                                   "--enable-url-protocols=http,https"
                                                   "--verbose"]}}
             :native-image [:cli :*native-image]
             :*uberjar {:aot :all
                        :dependencies
                        [[org.postgresql/postgresql "42.2.2"]]
                        :prep-tasks
                        [["omni" "compile"]
                         "compile"]}
             :uberjar [::server
                       :*uberjar]})
