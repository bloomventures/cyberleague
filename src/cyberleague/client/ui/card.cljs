(ns cyberleague.client.ui.card
  (:require
   [bloom.commons.fontawesome :as fa]
   [cyberleague.client.ui.error-boundary :as eb]
   [cyberleague.client.state :as state]))

(defn wrapper
  [{:keys [variant]} & children]
  [:div.card
   {:tw ["bg-white shrink-0 shadow-sm flex flex-col"
         (if (= variant :wide)
           "min-w-800px"
           "max-w-90vw")]}
   (for [c children]
     [eb/catch c])])

(defn header
  [{:keys [card refresh]} & content]
  [:header
   {:tw "bg-#3f51b5 text-white p-4 flex justify-between gap-4 items-center h-6"
    :style {:animation "flash 1s ease 0s 1 normal"}}
   (into [:<>] content)
   [:div.gap {:tw "grow"}]
   (when (seq refresh)
     [:button
      {:tw "text-white/65 hover:text-white p-2 -m-2"
       :on-click (fn [_]
                   (doseq [tada-atom refresh]
                     (state/refresh! tada-atom)))}
      [fa/fa-sync-solid {:tw "w-3 h-3"}]])
   [:button {:tw "text-white/65 hover:text-white p-2 -m-2"
             :on-click (fn [_]
                         (state/close-card! card))}
    [fa/fa-times-solid {:tw "w-3 h-3"}]]])

(defn body
  [{:keys [variant]} & content]
   [:div
    {:tw ["grow overflow-y-auto overflow-x-hidden flex"
          (if (= variant :code)
            ""
            "p-4 flex-col")]
     :style {:line-height 1.15}}
    (into [:<>] content)])
