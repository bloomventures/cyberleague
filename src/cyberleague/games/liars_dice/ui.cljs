(ns cyberleague.games.liars-dice.ui
  (:require
   [clojure.set :as set]
   [clojure.string :as string]
   [cyberleague.client.ui.common :as ui]
   [cyberleague.games.liars-dice.engine :as impl]
   [cyberleague.client.ui.colors :as colors]))

(defn dice-str [dice]
  (string/join " " (map str dice)))

(defn bot-name-for [player-idx bots player-mappings]
  (let [inv-mappings (set/map-invert player-mappings)]
    (->> bots
         (filter #(= (:bot/id %) (get inv-mappings player-idx)))
         first
         :bot/name)))

(defn die-view [die]
  [:span {:tw "text-2xl"}
   ({1 "⚀"
     2 "⚁"
     3 "⚂"
     4 "⚃"
     5 "⚄"
     6 "⚅"} die)])

(defn match-results-view
  [{:match/keys [bots winner player-mappings] :as match} states history]
  (let [inv-player-mappings (set/map-invert player-mappings)
        state (last states)]
    [:div.results.liars-dice {:tw "space-y-2"}
     [:table {:tw "w-full"}
      [:tbody
       (for [bot bots]
         ^{:key (:bot/id bot)}
         [:tr
          [:td
           [ui/bot-chip bot]]
          [:td
           (interpose
            " "
            (for [die (sort (get-in state [:dice (player-mappings (:bot/id bot))]))]
              [die-view die]))]
          [:td
           "("
           (interpose
            " "
            (for [die (sort (get-in state [:dice (player-mappings (:bot/id bot))]))]
              [:span {:tw "text"}
               die])) ")"]])]]

     (when (seq history)
       [:table {:tw "w-full text-sm"}
        [:thead
         [:tr
          [:th {:tw "text-left underline"} "Player"]
          [:th {:tw "text-left underline"} "Bid"]]]
        [:tbody
         (for [event history]
           [:tr
            [:td
             [ui/bot-chip (->> bots
                               (filter (fn [b]
                                         (= (:bot/id b)
                                            (inv-player-mappings (:player-id event)))))
                               first)]]
            [:td
             (case (-> event :move :action)
               "bid"
               [:span
                (-> event :move :quantity) " × "
                [die-view (-> event :move :face)]]
               "challenge"
               (str "LIAR!"
                    " ("
                    (if (= (-> event :player-id)
                           (impl/winner state))
                      "wins"
                      "loses")
                    ")")
               nil)]])]])]))
