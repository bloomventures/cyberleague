(ns cyberleague.db.matches
  (:require
   [malli.core :as m]
   [datomic.api :as d]
   [cyberleague.db.core :as db]
   [cyberleague.common.transit :as t]
   [cyberleague.common.schema :as s]))

(def matches-to-store-per-bot 25)

;; to prevent unbounded accumulation of match history
;; storing matches in a datommic no-history attribute

;; storing a copy of the match for each bot
;; keeping just {matches-to-store-per-bot} most recent matches per bot

;; to retrieve a match, you must specify a match-id AND a bot-id
;; it is likely for a match to retrievable for only some of the bots involved, but not all

(defn match-txs
  [match bot-ids]
  {:pre [(m/validate s/Match match)]}
  (let [existing-transit (->> (d/q '[:find ?bot-id ?bot-matches-transit
                                     :in $ [?bot-id ...]
                                     :where
                                     [?b :bot/id ?bot-id]
                                     [?b :bot/matches-transit ?bot-matches-transit]]
                                   (d/db db/*conn*)
                                   bot-ids)
                              (into {}))]
    (for [bot-id bot-ids]
      {:bot/id bot-id
       :bot/matches-transit (->> (or (-> (get existing-transit bot-id)
                                         t/read-str
                                         seq)
                                     '())
                                 (cons match)
                                 (take matches-to-store-per-bot)
                                 t/write-str)})))

