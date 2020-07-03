(ns cyberleague.coordinator.game-runner
  (:require
    [cyberleague.games.games :as games]))

(defn run-game [game bots]
  (let [g (games/make-engine game)
        nplayers (games/number-of-players g)
        simultaneous? (games/simultaneous-turns? g)
        parse-move (fn [m] (if (string? m) (read-string m) m))
        bots (->> bots
                  (map (fn [bot]
                         {:db/id (:db/id bot)
                          :bot/function (eval (read-string (:bot/deployed-code bot)))})))]
    (assert (= nplayers (count bots))
      (str "Wrong number of players (" (count bots) ") for " (:game/name game)))
    (try
      (loop [state (games/init-state g (map :db/id bots))
             players (cycle bots)]
        (if (games/game-over? g state)
          {:error false
           :winner (games/winner g state)
           :history (state "history")}
          (if simultaneous?
            ;; For simulatenous turns, get all the moves
            (let [moves (reduce
                          (fn [moves bot]
                            (let [move (try
                                         (parse-move
                                           ((:bot/function bot)
                                            (games/anonymize-state-for g (:db/id bot) state)))
                                         (catch Exception e
                                           (throw (ex-info "GameError"
                                                    {:error :exception-executing
                                                     :info (str e)
                                                     :bot (:db/id bot)
                                                     :game-state state}))))]
                              (cond
                                (not (games/valid-move? g move))
                                (throw (ex-info "GameError"
                                         {:error :invalid-move
                                          :move {:bot (:db/id bot)
                                                 :move move}
                                          :game-state state}))

                                (not (games/legal-move? g state (:db/id bot) move))
                                (throw (ex-info "GameError"
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
                            (games/anonymize-state-for g (:db/id bot) state)))
                         (catch Exception e
                           (throw (ex-info "GameError"
                                    {:error :exception-executing
                                     :info (str e)
                                     :bot (:db/id bot)
                                     :game-state state}))))]
              (cond
                (not (games/valid-move? g move))
                (throw (ex-info "GameError"
                         {:error :invalid-move
                          :move {:bot (:db/id bot)
                                 :move move}
                          :game-state state}))

                (not (games/legal-move? g state (:db/id bot) move))
                (throw (ex-info "GameError"
                         {:error :illegal-move
                          :move {:bot (:db/id bot)
                                 :move move}
                          :game-state state}))

                :else (recur (games/next-state g state {(:db/id bot) move})
                             (next players)))))))

      (catch Exception e
        (ex-data e)))))
