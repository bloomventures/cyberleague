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
   (let [log-entry (get log @move-index)
         game-state? (some? (:log-entry/state log-entry))]
     [:div.match-results
      {:tw "h-full overflow-y-auto overflow-x-hidden relative max-w-25em"}

      (when game-state?
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
                               (reset! move-index (js/parseInt (.. e -target -value) 10)))}]])

      [:div.content {:tw "p-2 space-y-2"}
       (when message
         [:p {:tw "text-center"}
          message])

       ;; per game custom view

       (when game-state?
         (if view
           (when (and (:log-entry/state log-entry)
                      (:match/player-mappings match))
             [eb/catch
              [view
               match
               (map :log-entry/state (take (inc @move-index) log))
               (:history (:log-entry/state (get log @move-index)))]])
           "CUSTOM VIEW NOT FOUND"))

       ;; generic state inspection view
       [:div.generic {:tw "space-y-2"}

        (when game-state?
          [:div
           [:div {:tw "text-white bg-#3f51b5 p-1"}
            "State (Private)"]
           [:code {:tw "block whitespace-pre-wrap py-1"}
            (-> (:log-entry/state log-entry)
                ;; the ui scrubber provides history
                ((fn [s]
                   (if (:history s)
                     (assoc s :history ["omitted"])
                     s)))
                clj->js
                (js/JSON.stringify nil 2))]])

        (for [bot (:match/bots match)
              :let [bot-id (:bot/id bot)]
              :when (or (get (:log-entry/contexts log-entry) bot-id)
                        (get (:log-entry/evals log-entry) bot-id))]
          [:div {:tw "space-y-2"}
           [:div {:tw "text-white bg-#3f51b5 p-1"}
            [ui/bot-chip bot]]

           (let [ping-pong-passed? (not= :eval.error.type/failed-ping-pong
                                         (-> (:log-entry/evals log-entry)
                                             (get bot-id)
                                             :eval/error
                                             :eval.error/type))]
             [:div {:tw "flex items-center gap-2 py-1"}
              [:span "Handshake:"]
              (if ping-pong-passed?
                [:span {:tw "text-green-700 font-bold"} "\u2713 passed"]
                [:span {:tw "text-red-700 font-bold"} "\u2717 failed"])])

           (when-let [ctx (get (:log-entry/contexts log-entry) bot-id)]
             [:div
              [ui/subheading "Context (stdin)"]
              [:code {:tw "block whitespace-pre-wrap py-1"}
               (-> ctx clj->js (js/JSON.stringify nil 2))]])

           (let [log (-> (:log-entry/evals log-entry)
                         (get bot-id)
                         :eval/stderr)]
             (when (not (string/blank? log))
               [:div
                [ui/subheading "Log (stderr)"]
                [:div {:tw "pl-2"}
                 [:code {:tw "block py-1 whitespace-pre-wrap"}
                  log]]]))

           (when-let [error (-> (:log-entry/evals log-entry)
                                (get bot-id)
                                :eval/error)]
             [:div
              [ui/subheading "Error"]
              [:div {:tw "pl-2"}
               [:code {:tw "block py-1 whitespace-pre-wrap"}
                (str error)]]])

           [:div
            [ui/subheading "Move (stdout) (raw)"]
            [:div {:tw "pl-2"}
             [:code {:tw "block py-1 whitespace-pre-wrap"}
              (-> (:log-entry/evals log-entry)
                  (get bot-id)
                  :eval/stdout)]]]

           [:div
            [ui/subheading "Move (stdout) (parsed)"]
            [:div {:tw "pl-2"}
             [:code {:tw "block py-1 whitespace-pre-wrap"}
              (try
                (-> (:log-entry/evals log-entry)
                    (get bot-id)
                    :eval/return-value
                    clj->js
                    (js/JSON.stringify nil 2))
                (catch js/Object _
                  "JSON PARSE ERROR"))]]]])]]])))

