(ns cyberleague.db.history
  (:require
   [datomic.api :as d]
   [cyberleague.db.core :as db]))

(defn last-n-ratings [bot-id n]
  (->> (d/q '[:find ?inst ?val
              :in $ ?id
              :where
              [?e :bot/id ?id]
              [?e :bot/rating ?val ?tx true]
              [?tx :db/txInstant ?inst]]
            (d/history (d/db db/*conn*))
            bot-id)
       (sort-by first)
       (take-last n)
       (mapv second)))

