(ns pog.core
  (:gen-class)
  (:require [pog.db :as db]
            [datomic.api :as d]
            [clojure.tools.nrepl.server :as nrepl]
            [pog.ranking :as ranking]
            [pog.game-runner :as game-runner]))

(defn -main
  [& [nrepl-port & args]]
  (try
    (let [port (Integer. nrepl-port)]
      (println "Starting nrepl on port " port)
      (nrepl/start-server :port port))
    (catch NumberFormatException ex
      (println "Not starting nrepl")))

  (println "Running games")
  (while true
    (doseq [[game all-bots] (db/with-conn (db/active-bots))]
      ; TODO:
      (when (= "ultimate tic-tac-toe" (:game/name game))
        (let [player-1 (rand-nth all-bots)
            player-2 (->> all-bots
                          (remove (partial = player-1))
                          (sort-by (fn [bot] (Math/abs
                                               (- (:bot/rating bot)
                                                  (:bot/rating player-1)))))
                          (take 10)
                          (sort-by :bot/rating-dev #(compare %2 %1))
                          (take 5)
                          rand-nth)
              _ (print "Starting " (:db/id player-1) " vs " (:db/id player-2) "...")
            result (game-runner/run-game
                     (into {} game)
                     (db/with-conn
                       [(-> (into {} player-1) (assoc :db/id (:db/id player-1)
                                                 :bot/deployed-code (db/deployed-code (:db/id player-1))))
                        (-> (into {} player-2) (assoc :db/id (:db/id player-2)
                                                 :bot/deployed-code (db/deployed-code (:db/id player-2))))]))]
        (println (str (:db/id player-1) " vs " (:db/id player-2) ": " (:winner result)))
        (if-not (:error result)
          ; TODO: handle ties?
          (let [match-info {:match/bots [(:db/id player-1) (:db/id player-2)]
                            :match/moves (pr-str (result :history))}
                match-info (if-let [winner (:winner result)]
                             (assoc match-info :match/winner winner)
                             match-info)]
            (db/with-conn
              (db/create-entity match-info)
              (ranking/update-rankings player-1 player-2 (:winner result))))
          (if (= (:error result) :exception-executing)
            (let [errd-bot (if (= (:db/id player-1) (:bot result))
                             player-1 player-2)]
              (println "Exception executing, will disable:" (:db/id errd-bot) (:info result))
              (db/with-conn
                (d/transact db/*conn*
                  [[:db/retract (:db/id errd-bot) :bot/code-version (:bot/code-version errd-bot)]])))
            (let [[winner cheater] (if (= (get-in result [:move :bot] (:db/id player-1)))
                                     [player-2 player-1]
                                     [player-1 player-2])]
              (println "Bad move from " cheater)
              (db/with-conn
                (db/create-entity {:match/bots [(:db/id player-1) (:db/id player-2)]
                                   :match/error true
                                   :match/moves (pr-str (conj (get-in result [:game-state "history"])
                                                              (get-in result [:move :move])))
                                   :match/winner (:db/id winner)})
                (d/transact db/*conn*
                  [[:db/add (:db/id cheater) :bot/rating (Math/max 0 (- 10 (:bot/rating cheater)))]
                   [:db/retract (:db/id cheater) :bot/code-version (:bot/code-version cheater)]]))))))))))
