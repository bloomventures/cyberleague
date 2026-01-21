(ns cyberleague.client.ui.match-results
  (:require
   [reagent.core :as r]
   [zprint.core :as zprint]
   [cyberleague.game-registrar :as registrar]))

(defn match-results-view
  [{:keys [message match]}]
  (r/with-let
    [view (get-in @registrar/games
                  [(get-in match [:match/game :game/name])
                   :game.config/match-results-view])
     state-history (match :match/state-history)
     max-value (dec (count state-history))
     move-index (r/atom max-value)]
    [:div.match-results
     {:tw "h-full overflow-y-auto relative"}

     [:div.scrubber {:tw "sticky top-0 bg-#9fa8da w-full p-2"}
      [:div.row {:tw "flex justify-between items-center"}
       [:button {:on-click (fn [] (swap! move-index dec))
                 :disabled (= 0 @move-index)}
        "<"]
       (str "Turn " @move-index "/" max-value)
       [:button {:on-click (fn [] (swap! move-index inc))
                 :disabled (= max-value @move-index)} ">"]]
      [:input {:type "range"
               :tw "w-full"
               :min 0
               :max max-value
               :step 1
               :value @move-index
               :on-change (fn [e]
                            (reset! move-index (js/parseInt (.. e -target -value) 10)))}]]

     [:div.content {:tw "p-2 space-y-2"}
      (when message
        [:p {:tw "text-center"}
         message])

      ;; per game custom view
      [view match
       (take (inc @move-index) state-history)
       (:history (get state-history @move-index))]

      ;; generic state inspection view
      [:div.generic

       [:div {:tw "font-bold border-black border-solid border-b"}
        "Move"]
       [:code {:tw "block py-1"}
        (pr-str (:move (last (:history (get state-history @move-index)))))]

       [:div {:tw "font-bold border-black border-solid border-b"}
        "State"]
       [:code {:tw "block whitespace-pre-wrap py-1"}
        (zprint/zprint-file-str
         (pr-str (-> (get state-history @move-index)
                     ;; the ui scrubber provides history
                     (assoc :history ["omitted"])))
         "reformat"
         {:width 33
          :style [#_:indent-only :community]})]
       (when (and 
               (:match/error match)
               (= @move-index max-value))
         [:<>
          [:div {:tw "font-bold border-black border-solid border-b"}
           "Error"]
          [:code {:tw "block whitespace-pre-wrap py-1"}
           (zprint/zprint-file-str
            (pr-str (:match/error match))
            "reformat"
            {:width 33
             :style [#_:indent-only :community]})]])]]]))

