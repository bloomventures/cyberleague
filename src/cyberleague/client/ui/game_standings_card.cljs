(ns cyberleague.client.ui.game-standings-card
  (:require
   [reagent.core :as r]
   [cyberleague.client.state :as state]
   [cyberleague.client.ui.card :as card]
   [cyberleague.client.ui.common :as ui]))

(defn make-interpolator [[x0 x1] [y0 y1]]
  (let [m (/ (- y1 y0)
             (- x1 x0))
        b (- y0 (* m x0))]
    (fn [x]
      (+ (* m x) b))))

(defn swarm-plot-view [{:keys [values x-min x-max size width]}]
  (let [r        (or size 3)
        svg-w    (or width 100)
        diameter (* 2 r)
        ->svg-x  (make-interpolator [x-min x-max] [r (- svg-w r)])
        placed   (reduce
                   (fn [acc v]
                     (let [x (->svg-x v)
                           y (loop [step 0]
                               (if (> step 100)
                                 0
                                 (let [y-try (if (zero? step)
                                               0
                                               (* (if (odd? step) 1 -1)
                                                  (quot (inc step) 2)
                                                  diameter))]
                                   (if (every? (fn [[px py]]
                                                 (let [dx (- x px)
                                                       dy (- y-try py)]
                                                   (> (js/Math.sqrt (+ (* dx dx) (* dy dy)))
                                                      diameter)))
                                               acc)
                                     y-try
                                     (recur (inc step))))))]
                       (conj acc [x y])))
                   []
                   (sort values))
        max-abs-y (if (empty? placed)
                    0
                    (apply max (map (fn [[_ y]] (js/Math.abs y)) placed)))
        svg-h    (max (+ (* 2 max-abs-y) diameter) diameter)
        center-y (+ max-abs-y r)]
    [:svg {:width svg-w
           :height svg-h
           :style {:overflow "visible"}}
     [:line {:x1 0 :x2 width :y1 max-abs-y :y2 max-abs-y :stroke "gray"}]
     (for [[i [x y]] (map-indexed vector placed)]
       ^{:key i}
       [:circle {:cx   x
                 :cy   (+ center-y y)
                 :r    r
                 :fill "#6877ca"}])]))

(defn standings-view
  [bots]
  (r/with-let
   [show-inactive? (r/atom false)]
   [:div {:tw "space-y-2"}
    [:table
     {:tw "mx-auto"}
     [:thead
      [:tr
       [:th {:tw "text-left font-bold p-1"}]
       [:th {:tw "text-left font-bold p-1"} "Rank"]
       [:th {:tw "text-left font-bold p-1"} "Bot"]
       [:th {:tw "text-left font-bold p-1"} "Rating"]
       [:th {:tw "text-left font-bold p-1"}]]]
     [:tbody
      (let [bots (->> bots
                      (filter (fn [bot]
                                (if @show-inactive?
                                  true
                                  (= :active (:bot/status bot))))))
            max-recent-rating (apply max (mapcat :bot/ratings-recent bots))]
        (for [[rank bot] (->> bots
                              (sort-by :bot/rating)
                              reverse
                              (map-indexed vector))]
          ^{:key (:bot/id bot)}
          [:tr
           [:td {:tw "p-1"} (when (= (:user/id (:bot/user bot)) (:user/id @state/user))
                              "★")]
           [:td {:tw "p-1"} (inc rank)]
           [:td {:tw "p-1"}
            [:a {:on-click (fn [_]
                             (state/nav! :card.type/bot (:bot/id bot)))}
             [ui/bot-chip bot]]]
           [:td {:tw "p-1"} (:bot/rating bot)]
           [:td {:tw "align-middle"}
            [swarm-plot-view {:values (:bot/ratings-recent bot)
                              :width 200
                              :size 1
                              :x-min 0
                              :x-max max-recent-rating}]]]))]]
    [ui/body-button {:on-click (fn []
                                 (swap! show-inactive? not))}
     (if @show-inactive?
       "Hide Inactive Bots"
       "Show Inactive Bots")]]))

(defn game-standings-card-view
  [[_ {:keys [id]} :as card]]
  (r/with-let
   [data (state/tada-atom [:api/game-standings {:game-id id}] {:refresh-rate 15000})]
   (when-let [game @data]
     [card/wrapper {}
      [card/header {:card card
                    :refresh [data]}
       [:<>
        [:span {:tw "whitespace-nowrap mr-4"}
         (str "#" (:game/slug game))
         " - "
         "Standings"]
        [:div {:tw "grow"}]]]
      [card/body {}
       [:div {:tw "max-w-45vw space-y-4"}
        [standings-view (:game/bots game)] ]]])))
