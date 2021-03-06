(ns cyberleague.coordinator.ranking
  (:require
   [clojure.math.numeric-tower :refer [expt]]
   [cyberleague.db.core :as db]))

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

(defn update-rankings! [p1 p2 winner]
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
    (db/update-rankings! p1 p1r p1rd p2 p2r p2rd)))
