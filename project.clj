(defproject cyberleague "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [com.datomic/datomic-free "0.9.5697"
                  :exclusions
                  [com.google.guava/guava]]
                 [io.bloomventures/commons "0.7.1"]
                 [io.bloomventures/omni "0.24.4"]

                 ;; client
                 [markdown-clj "1.10.4"]
                 [reagent "0.10.0"]
                 [org.clojure/clojurescript "1.10.764"]
                 [cljsjs/codemirror "5.44.0-1"]
                 [cljsjs/d3 "3.5.5-2"]

                 ;; coordinator
                 [org.clojure/math.numeric-tower "0.0.4"]]

  :main cyberleague.core

  :profiles {:uberjar {:aot :all}

             :dev {:repl-options {:init-ns cyberleague.core}}})
