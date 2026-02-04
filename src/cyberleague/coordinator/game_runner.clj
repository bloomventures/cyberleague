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
      (update :eval/return-value json/read-str :key-fn keyword)))

(defn run-move [bot state game-engine]
  (let [eval (try
               (eval-move bot (game-engine.protocol/anonymize-state-for game-engine (:db/id bot) state))
               (catch Exception e
                 {:eval/error {:move.error/type :move.error.type/invalid-code
                               :move.error/data {:message (str e)
                                                 :bot-id (:db/id bot)}}}))
        return-value (:eval/return-value eval)]
    (cond
      (:eval/error eval)
      eval

      (not (game-engine.protocol/valid-move? game-engine return-value))
      {:eval/error {:move.error/type :move.error.type/invalid-move
                    :move.error/data {:bot-id (:db/id bot)
                                      :move eval}}}

      (not (game-engine.protocol/legal-move? game-engine state (:db/id bot) return-value))
      {:eval/error {:move.error/type :move.error.type/illegal-move
                    :move.error/data {:bot-id (:db/id bot)
                                      :move eval}}}

      :else
      eval)))

(defn run-game [game bots]
  (let [game-engine (game-engine.protocol/make-engine game)
        nplayers (game-engine.protocol/number-of-players game-engine)]
    (assert (= nplayers (count bots))
            (str "Wrong number of players (" (count bots) ") for " (:game/name game)))
    (loop [states [(game-engine.protocol/init-state game-engine (map :db/id bots))]
           std-outs [""]
           players (cycle bots)]
      (let [state (last states)]
        (if (game-engine.protocol/game-over? game-engine state)
          {:game.result/error nil
           :game.result/winner (game-engine.protocol/winner game-engine state)
           :game.result/std-out-history std-outs
           :game.result/state-history states
           :game.result/history (state :history)}
          (if (game-engine.protocol/simultaneous-turns? game-engine)
            ;; For simulatenous turns, get all the moves
            (let [evals (->> (take nplayers players)
                             (map (fn [bot]
                                    [(:db/id bot)
                                     (run-move bot state game-engine)]))
                             (into {}))
                  errors (keep #(:eval/error (second %)) evals)]
              (if (seq errors)
                {:game.result/error errors
                 :game.result/winner nil
                 :game.result/std-out-history std-outs
                 :game.result/state-history states
                 :game.result/history (:history state)}
                (recur (conj states (game-engine.protocol/next-state game-engine state (update-vals evals :eval/return-value)))
                       (conj std-outs (update-vals evals :eval/std-out))
                       players)))
            ;; For one-at-a-time, just get the next player's move
            (let [bot (first players)
                  eval (run-move bot state game-engine)]
              (if (:eval/error eval)
                {:game.result/error (:eval/error eval)
                 :game.result/winner nil
                 :game.result/std-out-history std-outs
                 :game.result/state-history states
                 :game.result/history (:history state)}
                (recur (conj states
                             (game-engine.protocol/next-state game-engine state {(:db/id bot) (:eval/return-value eval)}))
                       (conj std-outs (:eval/std-out eval))
                       (next players))))))))))
