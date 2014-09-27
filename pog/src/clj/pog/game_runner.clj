(ns pog.game-runner
  (:require [me.raynes.fs :as fs]
            [cljs.closure :as cljsc]
            [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:import [javax.script ScriptEngineManager ScriptException]))

(defn eval-js
  [js]
  (let [engine (.getEngineByName (ScriptEngineManager. ) "nashorn")]
    (.eval engine js)))

(def game-runner-js "resources/precompiled.js") ; TODO

(defn precompile-game-runner
  []
  (let [output-dir "out/lib-precompile"] ; TODO
    (when (fs/directory? output-dir)
      (fs/delete-dir output-dir))
    (fs/mkdirs output-dir)
    (cljsc/build
      '[(ns pog.precompiled
          (:require cljs.reader
                    [pog.games :as games]))

        (deftype GameException [info]
          Object
          (getInfo [_] info))

        (defn ^:export run-game
          [game bots]
          (let [game (cljs.reader/read-string game)
                bots (cljs.reader/read-string bots)
                g (games/make-engine game)
                nplayers (games/number-of-players g)
                simultaneous? (games/simultaneous-turns? g)]
            (assert (= nplayers (count bots))
                    (str "Wrong number of players (" (count bots) ") for " (:game/name game)))
            (try
              (loop [state (games/init-state g (map :db/id bots))
                     players (cycle bots)]
                (if (games/game-over? g state)
                  (pr-str {:error false
                           :winner (games/winner g state)
                           :history (:history state)})
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

                                        :else (assoc moves (:db/id bot) move))))
                                  {}
                                  (take nplayers players))]
                      (recur (games/next-state g state moves) players))
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

                        :else (recur (games/next-state g state {(:db/id bot) move})
                                     (next players)))))))

              (catch GameException e
                (pr-str (.getInfo e))))))]
      {:optimizations :advanced
       :elide-asserts true
       :output-dir output-dir
       :output-to game-runner-js
       :pretty-print false})))

(def bots-dir "out/bots") ; TODO

(defn bot-namespace
  [{bot-id :db/id}]
  (symbol (str "bot-code-" bot-id)))

(defn precompile-bot
  [bot]
  (let [bot-dir (str bots-dir "/" (:db/id bot))
        filename (str bot-dir "/" (:db/id bot) "-" (:bot/code-version bot) ".js")]
    (when-not (fs/exists? filename)
      (when (fs/directory? bot-dir)
        (fs/delete-dir bot-dir))
      (fs/mkdirs bot-dir)
      (cljsc/build
        (vector
          (list 'ns (bot-namespace bot))
          (concat '(defn ^:export run) (rest (:bot/deployed-code bot))))
        {:optimizations :advanced
         :elide-asserts true
         :output-dir bot-dir
         :output-to filename
         :pretty-print false}))))

(defn code-for
  [{bot-id :db/id version :bot/code-version}]
  (slurp (str bots-dir "/" bot-id "/" bot-id "-" version ".js")))

(defn run-game
  [game bots]
  (when-not (fs/exists? game-runner-js)
    (precompile-game-runner))
  (doseq [bot bots]
    (precompile-bot bot))
  (edn/read-string
    (eval-js
      (clojure.string/join
        "\n"
        (concat
          [(slurp game-runner-js)]
          (map code-for bots)
          [(str "pog.precompiled.run_game("
                (pr-str (pr-str game))
                ","
                (->> bots
                     (map (fn [b] (assoc b :bot/function (symbol (name (bot-namespace b)) "run"))) )
                     pr-str
                     pr-str)
                ");")])))))
