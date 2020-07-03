(defproject cyberleague-coordinator "1.0.0"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [com.datomic/datomic-pro "0.9.4899"]
                 [org.clojure/math.numeric-tower "0.0.4"]
                 [me.raynes/fs "1.4.4"]
                 [org.clojure/clojurescript "1.7.145"]
                 [org.clojure/google-closure-library "0.0-20151016-61277aea"]
                 [org.clojure/tools.nrepl "0.2.3"]]

  :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                   :username "james@leanpixel.com"
                                   :password "eb6ce9b2-92f7-4164-86c3-49e60732bba9"}}
  :uberjar-name "player-of-games.jar"
  :main cyberleague.coordinator.core

  :source-paths ["src/clj" "src/cljs"]

  :profiles
  {:uberjar {:aot :all}
   :dev {:test-paths ["test/clj" "test/cljs"]
         :plugins [[quickie "0.2.5"]
                   [lein-cljsbuild "1.0.3"]
                   [com.cemerick/clojurescript.test "0.3.1"]]
         :cljsbuild {:builds [{:source-paths ["src/cljs" "test/cljs"]
                               :compiler {:output-to "target/cljs/testable.js"
                                          :optimizations :whitespace}}]
                     :test-commands {"unit-tests" ["jrunscript" "-f" "target/cljs/testable.js"
                                                   "-f" "resources/test/nashorn_runner.js"]}}}})
