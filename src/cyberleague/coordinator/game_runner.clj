(ns cyberleague.coordinator.game-runner
  (:require
   [clojure.data.json :as json]
   [cyberleague.server.evaluator-client :as eval-client]
   [cyberleague.games.protocol :as game-engine.protocol]))

(defn eval-move
  [artifact state]
  (if-let [result (eval-client/eval!
                   {:digest (:artifact/digest artifact)
                    :env-slug (:env/slug (:artifact/env artifact))
                    :input (json/write-str state)})]
    (assoc result :eval/return-value
           (json/read-str (:eval/stdout result) :key-fn keyword))
    nil))

#_(eval-move
   {:artifact/digest "878a289fd4cb8db5320e10fc9285a9ffd9a337e4e06468e7273385fa1e171c43"
    :artifact/env {:env/slug "clojure-sci"}}
   {:ping 551})

(defn run-move
  [player-index artifact state context game-engine]
  (let [eval (try
               (eval-move artifact context)
               ;; TODO this may fail for our reasons, not the bot's
               ;; ex. network failure; should handle it differently
               (catch Exception e
                 {:eval/error {:move.error/type :move.error.type/invalid-code
                               :move.error/data {:message (str e)}}}))
        return-value (:eval/return-value eval)]
    (cond
      ;; for the above try/catch
      (:eval/error eval)
      eval

      (not (game-engine.protocol/valid-move? game-engine return-value))
      (merge
       eval
       {:eval/error {:move.error/type :move.error.type/invalid-move}})

      (not (game-engine.protocol/legal-move? game-engine state player-index return-value))
      (merge
       eval
       {:eval/error {:move.error/type :move.error.type/illegal-move}})

      :else
      eval)))

(def Eval
  [:map
   [:eval/stdout :string]
   [:eval/stderr :string]
   ;; stdout json->edn, "move"
   [:eval/return-value :any]
   [:eval/error {:optional true}
    [:map
     [:move.error/type [:enum
                        :move.error.type/invalid-code
                        :move.error.type/invalid-move
                        :move.error.type/illegal-move]]]]])

(def PlayerId
  :int)

(def LogEntry
  [:map
   ;; state prior to moves
   [:log-entry/state :any]
   [:log-entry/contexts
    [:map-of PlayerId :any]]
   [:log-entry/evals
    [:map-of PlayerId Eval]]])

(defn run-game
  "Bots: [bot ...]
   Artifacts [artifact ...]"
  [{:keys [game bot-ids artifacts]}]
  (let [game-engine (game-engine.protocol/make-engine game)
        nplayers (game-engine.protocol/number-of-players game-engine)
        player-indexes (range (count bot-ids))
        bot-id->player-index (zipmap bot-ids
                                     player-indexes)]
    (assert (= nplayers (count bot-ids))
            (str "Wrong number of players (" (count bot-ids) ") for " (:game/name game)))
    (loop [state (game-engine.protocol/init-state game-engine player-indexes)
           log []
           player-indexes (cycle player-indexes)]
      (if (game-engine.protocol/game-over? game-engine state)
        {:game.result/errors {}
         :game.result/player-mappings bot-id->player-index
         :game.result/winner (get bot-ids (game-engine.protocol/winner game-engine state))
         :game.result/log (conj log
                                {:log-entry/state state})}
        (let [[player-indexes
               next-player-indexes]
              (if (game-engine.protocol/simultaneous-turns? game-engine)
                [player-indexes
                 player-indexes]
                [(take 1 player-indexes)
                 (next player-indexes)])
              contexts (->> (take nplayers player-indexes)
                            (map (fn [player-index]
                                   (let [bot-id (get bot-ids player-index)]
                                     [bot-id
                                      (game-engine.protocol/anonymize-state-for game-engine player-index state)])))
                            (into {}))
              evals (->> (take nplayers player-indexes)
                         (map (fn [player-index]
                                (let [bot-id (get bot-ids player-index)
                                      artifact (get artifacts player-index)
                                      context (get contexts bot-id)]
                                  [bot-id
                                   (run-move player-index artifact state context game-engine)])))
                         (into {}))
              errors (->> evals
                          (keep (fn [[bot-id result]]
                                  (when (:eval/error result)
                                    [bot-id (:eval/error result)])))
                          (into {}))
              moves (->> evals
                         (map (fn [[bot-id eval]]
                                [(bot-id->player-index bot-id)
                                 (:eval/return-value eval)]))
                         (into {}))
              next-log (conj log
                             {:log-entry/state state
                              :log-entry/contexts contexts
                              :log-entry/evals evals})]
          (if (seq errors)
            {:game.result/errors errors
             :game.result/player-mappings bot-id->player-index
             :game.result/winner nil
             :game.result/log next-log}
            (recur (game-engine.protocol/next-state game-engine state moves)
                   next-log
                   next-player-indexes)))))))
