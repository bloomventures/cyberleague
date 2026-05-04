(ns cyberleague.client.ui.match-results
  (:require
   [reagent.core :as r]
   [cyberleague.client.ui.common :as ui]
   [cyberleague.client.ui.error-boundary :as eb]
   [cyberleague.game-registrar :as games]))

(defn match-results-view
  [{:keys [message match]}]
  (r/with-let
   [view (:game.config/match-results-view (games/by-slug (get-in match [:match/game :game/slug])))
    state-history (:match/state-history match)
    std-out-history (:match/std-out-history match)
    max-value (dec (count state-history))
    move-index (r/atom max-value)]
   [:div.match-results
    {:tw "h-full overflow-y-auto overflow-x-hidden relative max-w-25em"}

    [:div.scrubber {:tw "sticky top-0 bg-#9fa8da w-full p-2"}

     [:div.row {:tw "flex justify-between items-center"}
      [:button {:on-click (fn [] (swap! move-index dec))
                :disabled (= 0 @move-index)}
       "<"]
      (str "Turn " (inc @move-index) "/" (inc max-value))
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

     (if view
       [eb/catch
        [view match
         (take (inc @move-index) state-history)
         (:history (get state-history @move-index))]]
       "CUSTOM VIEW NOT FOUND")

     ;; generic state inspection view
     [:div.generic {:tw "space-y-2"}

      [:div
       [:div {:tw "font-bold border-black border-solid border-b"}
        "Move"]
       (for [[bot-id move] (last (:history (get state-history @move-index)))
             :let [bot (->> (:match/bots match)
                            (filter (fn [b]
                                      (= bot-id (:bot/id b))))
                            first)]
             :when bot]
         [:div
          [:div {:tw "py-1 font-bold"}
           [ui/bot-chip bot]]
          [:code {:tw "block py-1 whitespace-pre-wrap"}
           (ui/pretty-print (pr-str move))]])]

      [:div
       [:div {:tw "font-bold border-black border-solid border-b"}
        "Log"]
       [:div {:tw "pl-2"}
        (for [[bot-id log] (get std-out-history @move-index)]
          [:div
           [:div {:tw "py-1 font-bold"}
            [ui/bot-chip (->> (:match/bots match)
                              (filter (fn [b]
                                        (= bot-id (:bot/id b))))
                              first)]]
           [:code {:tw "block py-1 whitespace-pre-wrap"}
            (ui/pretty-print (pr-str log))]])]]

      [:div
       [:div {:tw "font-bold border-black border-solid border-b"}
        "State"]
       [:code {:tw "block whitespace-pre-wrap py-1"}
        (ui/pretty-print
         (pr-str (-> (get state-history @move-index)
                     ;; the ui scrubber provides history
                     (assoc :history ["omitted"]))))]]

      (when (and
             (seq (:match/errors match))
             (= @move-index max-value))
        [:div
         [:div {:tw "font-bold border-black border-solid border-b"}
          "Errors"]
         [:div {:tw "pl-2"}
          (for [[bot-id error] (:match/errors match)]
            [:div
             [:div {:tw "py-1 font-bold"}
              [ui/bot-chip (->> (:match/bots match)
                                (filter (fn [b]
                                          (= bot-id (:bot/id b))))
                                first)]
              "(" (:move.error/type error) ")"]
             [:code {:tw "block whitespace-pre-wrap py-1"}
              (ui/pretty-print
               (pr-str (:move.error/data error)))]])]])]]]))

