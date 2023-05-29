(defproject cyberleague "0.1.0-SNAPSHOT"
  :dependencies [[io.bloomventures/omni "0.32.2"]
                 [com.datomic/peer "1.0.6733"
                  :exclusions
                  [com.google.guava/guava
                   commons-codec
                   org.clojure/tools.cli
                   org.slf4j/slf4j-nop
                   org.slf4j/jcl-over-slf4j
                   org.slf4j/slf4j-api]]


                 ;; client
                 [markdown-clj "1.10.4"]
                 #_[reagent "0.10.0"]
                 #_[org.clojure/clojurescript "1.10.764"]
                 [cljsjs/codemirror "5.44.0-1"]
                 [cljsjs/d3 "3.5.5-2"]

                 ;; coordinator
                 [org.clojure/math.numeric-tower "0.0.4"]
                 #_[org.clojure/data.json "1.0.0"]
                 [borkdude/sci "0.2.0"
                  :exclusions [org.clojure/tools.reader]]
                 ;; if need macos or windows, see:
                 ;; https://search.maven.org/search?q=g:com.eclipsesource.j2v8
                 [com.eclipsesource.j2v8/j2v8_linux_x86_64 "4.8.0"]

                 ;; registrar
                 [metosin/malli "0.2.1"]]

  :main cyberleague.core

  :omni-config cyberleague.omni-config/omni-config

  :source-paths ["src"]

  :test-paths ["test"]

  :profiles {:uberjar
             {:aot :all
              :dependencies
              [[org.postgresql/postgresql "42.2.2"]]
              :prep-tasks
              [["omni" "compile"]
               "compile"]}
             :dev {:repl-options {:init-ns cyberleague.core}}})
