(ns pog.ranking
  (:require [pog.db :as db]))

(def glicko-c 0.5)

(def glicko-q (/ (Math/log 10) 400))

(defn sq [x] (* x x))

(defn glicko
  [player-1 player-2 outcome]
  (letfn [(g [rd] (/ (Math/sqrt (+ 1 (* 3 (sq glicko-q) (sq rd) (/ (sq Math/PI)))))))
          (d2 [op-r op-rd]
            (/ (*  (sq glicko-q)
                  (sq (g op-rd))
                  )
               )
            )
          (new-rating [r rd op-r op-rd outcome]
            (+ r
               (/ glicko-q (+ (/ (sq rd))
                              (/ d2)
                              ))
               )
            ) ])
  )

(defn update-rankings
  [p1 p2 winner]
  (d/transact db/*conn*
    [[:db/add (:db/id p1) :bot/rating (+ (:bot/rating p1)
                                         (cond
                                           (= winner (:db/id p1)) 2
                                           (nil? winner) 1
                                           0))]
     [:db/add (:db/id p1) :bot/rating-dev (dec (:bot/rating-dev p1))]
     [:db/add (:db/id p2) :bot/rating (+ (:bot/rating p2)
                                         (cond
                                           (= winner (:db/id p2)) 2
                                           (nil? winner) 1
                                           0))]
     [:db/add (:db/id p2) :bot/rating-dev (dec (:bot/rating-dev p2))]]))
