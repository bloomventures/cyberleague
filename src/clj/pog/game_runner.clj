(ns pog.game-runner
  (:require [me.raynes.fs :as fs]
            [cljs.closure :as cljsc]
            [clojure.string :as string]
            [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:import [javax.script ScriptEngineManager ScriptContext ScriptException]))

(defn eval-js
  ([js] (eval-js js {}))
  ([js extra-bindings]
   (with-open [f (io/writer "out/debugging.js")]
     (.write f js))
   (let [engine (.getEngineByName (ScriptEngineManager. ) "nashorn")
         bindings (.getBindings engine ScriptContext/GLOBAL_SCOPE)]
     (doseq [[binding-name script] extra-bindings]
       (try
         (let [eng (.getEngineByName (ScriptEngineManager.) "nashorn")]
           (.put bindings binding-name (.eval eng script)))
         (catch ScriptException ex
           (println "Failed to add binding for " binding-name (.getMessage ex)))))
     (try
       (.eval engine js bindings)
       (catch ScriptException ex
         (println "Script exception at" (.getLineNumber ex) ":" (.getMessage ex)))))))

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

        (set-print-fn! js/print)

        (deftype GameException [info]
          Object
          (getInfo [_] info))

        (defn ^:export run-game
          [game bots bot-fns]
          (let [game (cljs.reader/read-string game)
                bots (->> (cljs.reader/read-string bots)
                          (map (fn [botfn bot] (assoc bot :bot/function botfn)) (seq bot-fns)))
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
                           :history (state "history")})
                  (if simultaneous?
                    ;; For simulatenous turns, get all the moves
                    (let [moves (reduce
                                  (fn [moves bot]
                                    (let [move (try
                                                 ((:bot/function bot)
                                                  (pr-str (games/anonymize-state-for g (:db/id bot) state)))
                                                 (catch :default e
                                                   (throw (GameException.
                                                            {:error :exception-executing
                                                             :info (str e)
                                                             :bot (:db/id bot)
                                                             :game-state state}))))]
                                      (cond
                                        (not (games/valid-move? g move))
                                        (throw (GameException.
                                                 {:error :invalid-move
                                                  :move {:bot (:db/id bot)
                                                         :move move}
                                                  :game-state state}))

                                        (not (games/legal-move? g state (:db/id bot) move))
                                        (throw (GameException.
                                                 {:error :illegal-move
                                                  :move {:bot (:db/id bot)
                                                         :move move}
                                                  :game-state state}))

                                        :else (assoc moves (:db/id bot) move))))
                                  {}
                                  (take nplayers players))]
                      (recur (games/next-state g state moves) players))
                    ;; For one-at-a-time, just get the next player's move
                    (let [bot (first players)
                          move (try
                                 ((:bot/function bot)
                                  (pr-str (games/anonymize-state-for g (:db/id bot) state)))
                                 (catch :default e
                                   (throw (GameException.
                                            {:error :exception-executing
                                             :info (str e)
                                             :bot (:db/id bot)
                                             :game-state state}))))]
                      (cond
                        (not (games/valid-move? g move))
                        (throw (GameException.
                                 {:error :invalid-move
                                  :move {:bot (:db/id bot)
                                          :move move}
                                  :game-state state}))

                        (not (games/legal-move? g state (:db/id bot) move))
                        (throw (GameException.
                                 {:error :illegal-move
                                  :move {:bot (:db/id bot)
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
       :pretty-print true})))

(def bots-dir "out/bots") ; TODO

(defn bot-namespace
  [{bot-id :db/id}]
  (symbol (str "bot-code-" bot-id)))

(defn js-bot-fn
  [{bot-id :db/id}]
  (str "bot_code_" bot-id "_run"))

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
          (list 'ns (bot-namespace bot)
            '(:require cljs.reader))
          '(set-print-fn! js/print)
          (concat '(defn ^:export bot-ai [])
                  [(list 'comp (:bot/deployed-code bot) 'cljs.reader/read-string)]))
        {:optimizations :advanced
         :elide-asserts true
         :output-dir bot-dir
         :output-to filename
         :pretty-print true}))))

(defn code-for
  [{bot-id :db/id version :bot/code-version :as bot}]
  (str (slurp (str bots-dir "/" bot-id "/" bot-id "-" version ".js"))
       (string/replace (bot-namespace bot) #"-" "_") ".bot_ai();"))

(defn tee [x] (println x) x)

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
          [(str "pog.precompiled.run_game("
                (pr-str (pr-str game))
                ","
                (pr-str (pr-str bots))
                ","
                "[" (clojure.string/join "," (map js-bot-fn bots)) "]"
                ");")]))
      (reduce (fn [a bot] (assoc a (js-bot-fn bot) (code-for bot)))
              {}
              bots))))
