(defproject cyberleague "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [com.datomic/datomic-free "0.9.5697"
                  :exclusions
                  [com.google.guava/guava]]
                 [io.bloomventures/commons "0.9.0"
                  :exclusions
                  [metosin/muuntaja]]
                 [io.bloomventures/omni "0.26.2"]

                 ;; client
                 [markdown-clj "1.10.4"]
                 [reagent "0.10.0"]
                 [org.clojure/clojurescript "1.10.764"]
                 [cljsjs/codemirror "5.44.0-1"]
                 [cljsjs/d3 "3.5.5-2"]

                 ;; coordinator
                 [org.clojure/math.numeric-tower "0.0.4"]
                 [org.clojure/data.json "1.0.0"]
                 [borkdude/sci "0.1.1-alpha.6"]

                 ;; registrar
                 [metosin/malli "0.0.1-20200719.212415-23"]]

  :main cyberleague.core

  :profiles {:uberjar {:aot :all}

             :dev {:repl-options {:init-ns cyberleague.core}}})
