(ns cyberleague.client.ui.match-results
  (:require
   [clojure.string :as string]
   [reagent.core :as r]
   [cyberleague.client.ui.common :as ui]
   [cyberleague.client.ui.error-boundary :as eb]
   [cyberleague.game-registrar :as games]))

(defn match-results-view
  [{:keys [message match]}]
  (r/with-let
   [view (:game.config/match-results-view (games/by-slug (get-in match [:match/game :game/slug])))
    log (:match/log match)
    max-value (dec (count log))
    move-index (r/atom max-value)]
   (let [log-entry (get log @move-index)]
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
          [view
           match
           (map :log-entry/state (take (inc @move-index) log))
           (:history (:log-entry/state (get log @move-index)))]]
         "CUSTOM VIEW NOT FOUND")

       ;; generic state inspection view
       [:div.generic {:tw "space-y-2"}

        [:div
         [:div {:tw "text-white bg-#3f51b5 p-1"}
          "State "]
         [:code {:tw "block whitespace-pre-wrap py-1"}
          (-> (:log-entry/state log-entry)
              ;; the ui scrubber provides history
              ((fn [s]
                 (if (:history s)
                   (assoc s :history ["omitted"])
                   s)))
              clj->js
              (js/JSON.stringify nil 2))]]

        (for [bot (:match/bots match)]
          [:div
           [:div {:tw "text-white bg-#3f51b5 p-1"}
            [ui/bot-chip bot]]

           [:div
            [:div {:tw "font-bold border-black border-solid border-b"}
             "Context (stdin)"]
            [:code {:tw "block whitespace-pre-wrap py-1"}
             (-> (:log-entry/contexts log-entry)
                 (get (:bot/id bot))
                 clj->js
                 (js/JSON.stringify nil 2))]]

           (let [log (-> (:log-entry/evals log-entry)
                         (get (:bot/id bot))
                         :eval/stderr)]
             (when (not (string/blank? log))
               [:div
                [:div {:tw "font-bold border-black border-solid border-b"}
                 "Log (stderr)"]
                [:div {:tw "pl-2"}
                 [:code {:tw "block py-1 whitespace-pre-wrap"}
                  log]]]))

           (when-let [error (-> (:log-entry/evals log-entry)
                                (get (:bot/id bot))
                                :eval/error)]
             [:div
              [:div {:tw "font-bold border-black border-solid border-b"}
               "Error"]
              [:div {:tw "pl-2"}
               [:code {:tw "block py-1 whitespace-pre-wrap"}
                (str error)]]])

           [:div
            [:div {:tw "font-bold border-black border-solid border-b"}
             "Move (stdout)"]
            [:div {:tw "pl-2"}
             [:code {:tw "block py-1 whitespace-pre-wrap"}
              (-> (:log-entry/evals log-entry)
                  (get (:bot/id bot))
                  :eval/return-value
                  clj->js
                  (js/JSON.stringify nil 2))]]]])]]])))

