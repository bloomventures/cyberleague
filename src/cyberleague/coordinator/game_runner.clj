(ns cyberleague.coordinator.game-runner
  (:require
   [clojure.data.json :as json]
   [cyberleague.coordinator.evaluators.api :as evaluator.api]
   [cyberleague.coordinator.evaluators.clojure]
   [cyberleague.coordinator.evaluators.javascript]
   [cyberleague.games.protocol :as game-engine.protocol]))

(defn eval-move
  [bot state]
  (-> state
      json/write-str
      (evaluator.api/native-code-runner (get-in bot [:bot/code :code/language])
                                        (get-in bot [:bot/code :code/code]))
      (json/read-str :key-fn keyword)))

(defn run-move [bot state game-engine]
  (let [move (try
               (eval-move bot (game-engine.protocol/anonymize-state-for game-engine (:db/id bot) state))
               (catch Exception e
                 (throw (ex-info "GameError"
                                 {:error :exception-executing
                                  :info (str e)
                                  :bot (:db/id bot)
                                  :game-state state}))))]
    (cond
      (not (game-engine.protocol/valid-move? game-engine move))
      (throw (ex-info "GameError"
                      {:error :invalid-move
                       :move {:bot (:db/id bot)
                              :move move}
                       :game-state state}))

      (not (game-engine.protocol/legal-move? game-engine state (:db/id bot) move))
      (throw (ex-info "GameError"
                      {:error :illegal-move
                       :move {:bot (:db/id bot)
                              :move move}
                       :game-state state}))

      :else
      move)))

(defn run-game [game bots]
  (let [game-engine (game-engine.protocol/make-engine game)
        nplayers (game-engine.protocol/number-of-players game-engine)]
    (assert (= nplayers (count bots))
            (str "Wrong number of players (" (count bots) ") for " (:game/name game)))
    (loop [states [(game-engine.protocol/init-state game-engine (map :db/id bots))]
           players (cycle bots)]
      (let [state (last states)]
        (if (game-engine.protocol/game-over? game-engine state)
          {:game.result/error false
           :game.result/winner (game-engine.protocol/winner game-engine state)
           :game.result/state-history states
           :game.result/history (state :history)}
          (if (game-engine.protocol/simultaneous-turns? game-engine)
            ;; For simulatenous turns, get all the moves
            (let [moves (->> (take nplayers players)
                             (map (fn [bot]
                                    [(:db/id bot)
                                     (try
                                       (run-move bot state game-engine)
                                       (catch Exception e
                                         {::error (ex-data e)}))]))
                             (into {}))]
              (if (some #(contains? (second %) ::error) moves)
                {:game.result/error (map ::error moves)
                 :game.result/winner nil
                 :game.result/state-history states
                 :game.result/history (:history (last states))}
                (recur (conj states (game-engine.protocol/next-state game-engine state moves))
                       players)))
            ;; For one-at-a-time, just get the next player's move
            (let [bot (first players)
                  move (try
                         (run-move bot state game-engine)
                         (catch Exception e
                           {::error (ex-data e)}))]
              (if (::error move)
                {:game.result/error (::error move)
                 :game.result/winner nil
                 :game.result/state-history states
                 :game.result/history (:history (last states))}
                (recur (conj states (game-engine.protocol/next-state game-engine state {(:db/id bot) move}))
                       (next players))))))))))
