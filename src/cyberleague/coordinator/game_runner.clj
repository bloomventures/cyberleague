(ns cyberleague.coordinator.game-runner
  (:require
   [clojure.data.json :as json]
   [cyberleague.server.evaluator-client :as eval-client]
   [cyberleague.games.protocol :as game-engine.protocol]))

(defn eval-move
  [artifact state]
  (let [result (eval-client/eval!
                {:digest (:artifact/digest artifact)
                 :env-slug (:env/slug (:artifact/env artifact))
                 :input (json/write-str state)})]
    (update result :eval/return-value json/read-str :key-fn keyword)))

(defn run-move
  [bot-id artifact state game-engine]
  (let [eval (try
               (->> (game-engine.protocol/anonymize-state-for game-engine bot-id state)
                    (eval-move artifact))
               (catch Exception e
                 {:eval/error {:move.error/type :move.error.type/invalid-code
                               :move.error/data {:message (str e)
                                                 :bot-id bot-id}}}))
        return-value (:eval/return-value eval)]
    (cond
      (:eval/error eval)
      eval

      (not (game-engine.protocol/valid-move? game-engine return-value))
      {:eval/error {:move.error/type :move.error.type/invalid-move
                    :move.error/data {:bot-id bot-id
                                      :move eval}}}

      (not (game-engine.protocol/legal-move? game-engine state bot-id return-value))
      {:eval/error {:move.error/type :move.error.type/illegal-move
                    :move.error/data {:bot-id bot-id
                                      :move eval}}}

      :else
      eval)))

(defn run-game
  "Bots: [bot ...]
   Artifacts [artifact ...]"
  [{:keys [game bot-ids artifacts]}]
  (let [game-engine (game-engine.protocol/make-engine game)
        nplayers (game-engine.protocol/number-of-players game-engine)]
    (assert (= nplayers (count bot-ids))
            (str "Wrong number of players (" (count bot-ids) ") for " (:game/name game)))
    (loop [states [(game-engine.protocol/init-state game-engine bot-ids)]
           std-outs [""]
           player-indexes (cycle (range (count bot-ids)))]
      (let [state (last states)]
        (if (game-engine.protocol/game-over? game-engine state)
          {:game.result/error nil
           :game.result/winner (game-engine.protocol/winner game-engine state)
           :game.result/std-out-history std-outs
           :game.result/state-history states
           :game.result/history (state :history)}
          (if (game-engine.protocol/simultaneous-turns? game-engine)
            ;; For simultaneous turns, get all the moves
            (let [evals (->> (take nplayers player-indexes)
                             (map (fn [player-index]
                                    (let [bot-id (get bot-ids player-index)
                                          artifact (get artifacts player-index)]
                                      [bot-id
                                       (run-move bot-id artifact state game-engine)])))
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
                       player-indexes)))
            ;; For one-at-a-time, just get the next player's move
            (let [player-index (first player-indexes)
                  bot-id (get bot-ids player-index)
                  artifact (get artifacts player-index)
                  eval (run-move bot-id artifact state game-engine)]
              (if (:eval/error eval)
                {:game.result/error (:eval/error eval)
                 :game.result/winner nil
                 :game.result/std-out-history std-outs
                 :game.result/state-history states
                 :game.result/history (:history state)}
                (recur (conj states
                             (game-engine.protocol/next-state game-engine state {bot-id (:eval/return-value eval)}))
                       (conj std-outs (:eval/std-out eval))
                       (next player-indexes))))))))))
