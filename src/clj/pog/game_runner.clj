(ns pog.game-runner
  (:require [me.raynes.fs :as fs]
            [cljs.closure :as cljsc]
            [clojure.string :as string]
            [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:import [javax.script ScriptEngineManager ScriptContext ScriptException]))

(defn edn->js
  []
  (let [output-file "resources/edn_to_js.js"]
    (when-not (fs/exists? output-file)
      (let [output-dir "out/edn-precompile"]
        (when (fs/directory? output-dir)
          (fs/delete-dir output-dir))
        (fs/mkdirs output-dir)
        (cljsc/build
          '[(ns pog.edn-to-js
              (:require cljs.reader))
            (defn ^:export edn-to-json-fn []
              (comp clj->js cljs.reader/read-string))]
          {:optimizations :advanced
           :elide-asserts true
           :output-dir output-dir
           :output-to output-file
           :pretty-print false})))
    (str (slurp output-file) "pog.edn_to_js.edn_to_json_fn();")))

(defn eval-js
  ([js] (eval-js js {}))
  ([js extra-bindings]
   (let [engine (.getEngineByName (ScriptEngineManager. ) "nashorn")
         bindings (.getBindings engine ScriptContext/GLOBAL_SCOPE)
         inner-bindings (let [eng (.getEngineByName (ScriptEngineManager.) "nashorn")]
                          (doto (.getBindings eng ScriptContext/GLOBAL_SCOPE)
                            (.put "edn_to_json" (.eval eng (edn->js)))))
         writers (atom {})]
     (doseq [[binding-name script] extra-bindings]
       (try
         (let [eng (.getEngineByName (ScriptEngineManager.) "nashorn")
               writer (java.io.StringWriter. )]
           (swap! writers assoc binding-name writer)
           (.. eng getContext (setWriter writer))
           (.put bindings binding-name (.eval eng script inner-bindings)))
         (catch ScriptException ex
           (println "Failed to add binding for " binding-name (.getMessage ex)))))
     (try
       (let [result (.eval engine js bindings)
             output (reduce-kv #(assoc %1 %2 (str %3)) {} @writers)]
         [result output])
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
                simultaneous? (games/simultaneous-turns? g)
                parse-move (fn [m] (if (string? m) (cljs.reader/read-string m) m))]
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
                                                 (parse-move
                                                   ((:bot/function bot)
                                                    (pr-str (games/anonymize-state-for g (:db/id bot) state))))
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
                                 (parse-move
                                   ((:bot/function bot)
                                    (pr-str (games/anonymize-state-for g (:db/id bot) state))))
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

(defmulti precompile-bot (fn [bot] (get-in bot [:bot/code :code/language])))

(defmethod precompile-bot :default ; clojurescript
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
                  [(list 'comp (edn/read-string (:bot/deployed-code bot)) 'cljs.reader/read-string)]))
        {:optimizations :advanced
         :elide-asserts true
         :output-dir bot-dir
         :output-to filename
         :pretty-print true}))))

(defmethod precompile-bot "javascript"
  [bot]
  nil)

(defmulti code-for (fn [bot] (get-in bot [:bot/code :code/language])))

(defmethod code-for :default
  [{bot-id :db/id version :bot/code-version :as bot}]
  (str (slurp (str bots-dir "/" bot-id "/" bot-id "-" version ".js"))
         (string/replace (bot-namespace bot) #"-" "_") ".bot_ai();"))

(defmethod code-for "javascript"
  [{code :bot/deployed-code :as bot}]
  code)

(defmacro with-timeout
  "Evaluate the body, returning else if body fails to complete in s seconds"
  [s body else]
  `(let [f# (future ~body)]
     (try
       (.get f# ~s java.util.concurrent.TimeUnit/SECONDS)
       (catch java.util.concurrent.TimeoutException ex#
         (future-cancel f#)
         ~else))))

(defn run-game
  [game bots]
  (when-not (fs/exists? game-runner-js)
    (precompile-game-runner))
  (doseq [bot bots]
    (precompile-bot bot))
  (with-timeout 60
    (let [[result output] (eval-js
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
                                    bots))]
      (assoc (edn/read-string result) :output output))
    (do (println "Timeout")
        {:error :timeout-executing
         :info "Took too long to complete"})))
