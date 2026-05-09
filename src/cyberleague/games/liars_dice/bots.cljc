(ns cyberleague.games.liars-dice.bots)

;; A bot that bids and challenges randomly without any strategy
(def random-bot
  '(fn [state]
     (if (:ping state)
       {:pong (:ping state)}
       (let [{:keys [my-dice history]} state
             last-bid (->> history (filter #(= "bid" (-> % :move :action))) last)
             total-dice (* 2 (count my-dice))]
         (if (nil? last-bid)
           ;; No current bid: must bid
           {:action "bid"
            :quantity 1
            :face (inc (rand-int 6))}
           ;; Existing bid: challenge 30% of the time, otherwise raise
           (if (< (rand) 0.3)
             {:action "challenge"}
             (let [q (-> last-bid :move :quantity)
                   f (-> last-bid :move :face)
                   same-qty-bids (for [nf (range (inc f) 7)]
                                   {:action "bid" :quantity q :face nf})
                   higher-qty-bids (for [nq (range (inc q) (inc total-dice))
                                         nf (range 1 7)]
                                     {:action "bid" :quantity nq :face nf})
                   valid-bids (vec (concat same-qty-bids higher-qty-bids))]
               (if (seq valid-bids)
                 (rand-nth valid-bids)
                 {:action "challenge"}))))))))

;; A bot that counts its own dice and makes probabilistic decisions
(def counting-bot
  '(fn [state]
     (if (:ping state)
       {:pong (:ping state)}
       (let [{:keys [my-dice history]} state
             last-bid (->> history (filter #(= "bid" (-> % :move :action))) last)
             total-dice (* 2 (count my-dice))
             my-face-count (fn [face]
                             (count (filter #(or (= face %) (= 1 %)) my-dice)))]
         (if (nil? last-bid)
           ;; First bid: pick the face we have most of
           (let [best-face (->> (range 2 7)
                                (sort-by (fn [f] (- (my-face-count f))))
                                first)
                 count-of-best (my-face-count best-face)]
             {:action "bid"
              :quantity count-of-best
              :face best-face})
           ;; Existing bid: challenge if unlikely, else raise quantity
           (let [q (-> last-bid :move :quantity)
                 f (-> last-bid :move :face)
                 my-count (my-face-count f)
                 expected (int (* total-dice (/ 1.0 3)))]
             (if (> q (+ my-count expected))
               {:action "challenge"}
               {:action "bid"
                :quantity (inc q)
                :face f})))))))
