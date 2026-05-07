(ns cyberleague.coordinator.ranking
  (:require
   [clojure.math.numeric-tower :refer [expt]]))

(def PI Math/PI)

(defn sq [x] (expt x 2))

;; c: per-game volatility — inflates RD before each match (the Glicko time-inflation
;; mechanism, applied per game rather than per period). Prevents RD from converging
;; to zero; keeps ratings responsive even for bots with thousands of games played.
;; Tune upward if ratings feel too sticky, downward if too volatile.
(def glicko-volatility 25)

(defn glicko [p1r p1rd p2r p2rd outcome]
  (let [s (case outcome
            :win 1
            :loss 0
            :tie 0.5)
        R1 p1r
        R2 p2r
        ;; inflate RD by volatility before computing, so uncertainty never fully drains
        RD1 (Math/sqrt (+ (sq p1rd) (sq glicko-volatility)))
        RD2 (Math/sqrt (+ (sq p2rd) (sq glicko-volatility)))
        q 0.0057565
        g (fn [RD] (expt (+ 1 (* 3 q q RD RD (/ PI) (/ PI))) -1/2))
        E (/ (+ 1 (expt 10 (* -1 (g RD2) (- R1 R2) (/ 400)))))
        dsq (/ (* (sq q) (sq (g RD2)) E (- 1 E)))
        new-R1 (+ R1 (* (/ q (+ (/ (sq RD1)) (/ dsq))) (g RD2) (- s E)))
        new-RD1 (expt (+ (/ (sq RD1)) (/ dsq)) -1/2)]
    [new-R1 new-RD1]))

(defn new-ratings [p1 p2 winner]
  (let [[p1r p1rd] (glicko
                    (:bot/rating p1) (:bot/rating-dev p1)
                    (:bot/rating p2) (:bot/rating-dev p2)
                    (cond
                      (= winner (:bot/id p1)) :win
                      (nil? winner) :tie
                      :else :loss))
        [p2r p2rd] (glicko
                    (:bot/rating p2) (:bot/rating-dev p2)
                    (:bot/rating p1) (:bot/rating-dev p1)
                    (cond
                      (= winner (:bot/id p2)) :win
                      (nil? winner) :tie
                      :else :loss))]
    [{:bot/id (:bot/id p1)
      :bot/rating (Math/max 0 (Math/round p1r))
      :bot/rating-dev (Math/max 100 (Math/round p1rd))}
     {:bot/id (:bot/id p2)
      :bot/rating (Math/max 0 (Math/round p2r))
      :bot/rating-dev (Math/max 100 (Math/round p2rd))}]))
