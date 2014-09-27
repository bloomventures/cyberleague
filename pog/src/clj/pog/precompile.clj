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
            (:require [pog.games :as games]))

          (deftype GameException [info]
            Object
            (getInfo [_] info))

          (defn ^:export
            [game bots]
            (let [g (games/make-engine game)
                  nplayers (games/number-of-players g)
                  simultaneous? (games/simultaneous-turns?)]
              (assert (= nplayers (count bots))
                      (str "Wrong number of players (" (count bots) ") for " (:game/name game)))
              (try
                (loop [state (games/init-state g)
                       players (cycle bots)]
                  (if (games/game-over? g state)
                    (games/winner g state)
                    (if simultaneous?
                      ;; For simulatenous turns, get all the moves
                      (let [moves (reduce
                                    (fn [moves bot]
                                      (let [move ((:bot/function bot) state)]
                                        (cond
                                          (not (games/valid-move? g move))
                                          (throw (GameException.
                                                   {:error true
                                                    :invalid-move {:bot (:db/id bot)
                                                                   :move move}
                                                    :game-state state}))

                                          (not (games/legal-move? g state (:db/id bot) move))
                                          (throw (GameException.
                                                   {:error true
                                                    :illegal-move {:bot (:db/id bot)
                                                                   :move move}
                                                    :game-state state}))

                                          (assoc moves (:db/id bot) move))))
                                    {}
                                    (take nplayers players))]
                        (recur (games/next-state state moves) players))
                      ;; For one-at-a-time, just get the next player's move
                      (let [bot (first players)
                            move ((:bot/function bot) state)]
                        (cond
                          (not (games/valid-move? g move))
                          (throw (GameException.
                                   {:error true
                                    :invalid-move {:bot (:db/id bot)
                                                   :move move}
                                    :game-state state}))

                          (not (games/legal-move? g state (:db/id bot) move))
                          (throw (GameException.
                                   {:error true
                                    :illegal-move {:bot (:db/id bot)
                                                   :move move}
                                    :game-state state}))

                          (recur (games/next-state state {(:db/id bot) move})
                                 (next players)))))))

                (catch GameException e
                  (.getInfo e)))))]
        {:optimizations :advanced
         :elide-asserts true
         :output-dir output-dir
         :output-to "resources/precompiled.js" ; todo
         :pretty-print false}))))

