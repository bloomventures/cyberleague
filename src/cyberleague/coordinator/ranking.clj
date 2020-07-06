(ns cyberleague.coordinator.ranking
  (:require
   [clojure.math.numeric-tower :refer [expt]]
   [datomic.api :as d]
   [cyberleague.coordinator.db :as db]))

(def PI Math/PI)

(defn sq [x] (expt x 2))

(defn glicko [p1r p1rd p2r p2rd outcome]

  (let [s (case outcome
            :win 1
            :loss 0
            :tie 0.5)
        R1 p1r
        R2 p2r
        RD1 p1rd
        RD2 p2rd
        q 0.0057565
        g (fn [RD] (expt (+ 1 (* 3 q q RD RD (/ PI) (/ PI))) -1/2))
        E (/ (+ 1 (expt 10 (* -1 (g RD2) (- R1 R2) (/ 400)))))
        dsq (/ (* (sq q) (sq (g RD2)) E (- 1 E)))
        new-R1 (+ R1 (* (/ q (+ (/ (sq RD1)) (/ dsq ))) (g RD2) (- s E)))
        new-RD1 (expt (+ (/ (sq RD1)) (/ dsq)) -1/2)]

    [new-R1 new-RD1]))

(defn update-rankings [p1 p2 winner]
  (let [[p1r p1rd] (glicko
                     (:bot/rating p1) (:bot/rating-dev p1)
                     (:bot/rating p2) (:bot/rating-dev p2)
                     (cond
                       (= winner (:db/id p1)) :win
                       (nil? winner) :tie
                       :else :loss))
        [p2r p2rd] (glicko
                     (:bot/rating p2) (:bot/rating-dev p2)
                     (:bot/rating p1) (:bot/rating-dev p1)
                     (cond
                       (= winner (:db/id p2)) :win
                       (nil? winner) :tie
                       :else :loss))]
    (d/transact db/*conn*
      [[:db/add (:db/id p1) :bot/rating (Math/max 0 (Math/round p1r))]
       [:db/add (:db/id p2) :bot/rating (Math/max 0 (Math/round p2r))]
       [:db/add (:db/id p1) :bot/rating-dev (Math/max 50 (Math/round p1rd))]
       [:db/add (:db/id p2) :bot/rating-dev (Math/max 50 (Math/round p2rd))]])))


#_(defn update-rankings
  [p1 p2 winner]
  (d/transact db/*conn*
    [[:db/add (:db/id p1) :bot/rating (+ (:bot/rating p1)
                                         (cond
                                           (= winner (:db/id p1)) 2
                                           (nil? winner) 1
                                           :else 0))]
     [:db/add (:db/id p1) :bot/rating-dev (dec (:bot/rating-dev p1))]
     [:db/add (:db/id p2) :bot/rating (+ (:bot/rating p2)
                                         (cond
                                           (= winner (:db/id p2)) 2
                                           (nil? winner) 1
                                           :else 0))]
     [:db/add (:db/id p2) :bot/rating-dev (dec (:bot/rating-dev p2))]]))
