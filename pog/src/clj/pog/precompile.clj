(ns pog.precompile
  (:require [me.raynes.fs :as fs]
            [cljs.closure :as cljsc]
            [clojure.java.io :as io])
  (:import [javax.script ScriptEngineManager ScriptException]))

(defn eval-js
  [js]
  (let [engine (.getEngineByName (ScriptEngineManager. ) "nashorn")]
    (.eval engine js)))

(defn precompile-game-runner
  []
  (let [output-dir "out/lib-precompile"]
    (when (fs/directory? output-dir)
      (fs/delete-dir output-dir))
    (fs/mkdirs output-dir)
    (str
      (cljsc/build
        '[(ns pog.precompiled
            (:require cljs.reader
                      [pog.games :as games]))

          (defn ^:export
            [game bots]
            (let [g (games/make-engine game)]
              (loop [state (games/init-state g)
                     players (cycle bots)]
                (if (games/game-over? g state))
                )))]
        {:optimizations :advanced
         :elide-asserts true
         :output-dir output-dir
         :output-to "resources/precompiled.js" ; todo
         :pretty-print false}))))

