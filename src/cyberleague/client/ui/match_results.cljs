(ns cyberleague.client.ui.match-results
  (:require
   [reagent.core :as r]
   [zprint.core :as zprint]
   [cyberleague.game-registrar :as registrar]
   [cyberleague.client.ui.error-boundary :as eb]))

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
     {:tw "h-full overflow-y-auto overflow-x-hidden relative max-w-25em"}

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

      [eb/catch
       [view match
        (take (inc @move-index) state-history)
        (:history (get state-history @move-index))]]

      ;; generic state inspection view
      [:div.generic {:tw "space-y-2"}

       [:div
        [:div {:tw "font-bold border-black border-solid border-b"}
         "Move"]
        [:code {:tw "block py-1"}
         (pr-str (:move (last (:history (get state-history @move-index)))))]]

       [:div
        [:div {:tw "font-bold border-black border-solid border-b"}
         "State"]
        [:code {:tw "block whitespace-pre-wrap py-1"}
         (zprint/zprint-file-str
          (pr-str (-> (get state-history @move-index)
                      ;; the ui scrubber provides history
                      (assoc :history ["omitted"])))
          "reformat"
          {:width 33
           :style [:community]})]]

       (when (and
              (:match/error match)
              (= @move-index max-value))
         (for [error
               ;; in parallel games, :match/error is a collection
               (if (sequential? (:match/error match))
                 (:match/error match)
                 [(:match/error match)])]
           [:div
            [:div {:tw "font-bold border-black border-solid border-b"}
             "Error" " (" (:move.error/type error) ")"]
            [:code {:tw "block whitespace-pre-wrap py-1"}
             (zprint/zprint-file-str
              (pr-str (:move.error/data error))
              "reformat"
              {:width 33
               :style [:community]})]]))]]]))

