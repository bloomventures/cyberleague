(ns cyberleague.client.ui.code-card
  (:require
   [bloom.commons.debounce :refer [debounce]]
   [reagent.core :as r]
   [cyberleague.client.state :as state]
   [cyberleague.client.ui.card :as card]
   [cyberleague.client.ui.code-editor :refer [code-editor-view]]
   [cyberleague.client.ui.common :as ui]
   [cyberleague.client.ui.test :refer [test-view]]))

(defn code-card-view
  [[_ {:keys [id]} :as card]]
  (r/with-let
    [data (state/tada-atom [:api/bot-code {:bot-id id}])
     status (r/atom :saved) ; :saved :editing :saving :testing :passed/:failed :deploying :deployed
     bot-id id
     test-match (r/atom nil)
     save! (fn [value]
             (reset! status :saving)
             (-> (state/tada! [:api/set-bot-code! {:bot-id bot-id :code value}])
                 (.then (fn [_]
                          (reset! status :saved)))))
     debounced-save! (debounce save! 750)
     test! (fn []
             (reset! test-match nil)
             (reset! status :testing)
             (-> (state/tada! [:api/test-bot! {:bot-id bot-id}])
                 (.then (fn [match]
                          (reset! status (if (:match/error match) :failed :passed))
                          (reset! test-match match)))))
     deploy! (fn []
               (reset! status :deploying)
               (-> (state/tada! [:api/deploy-bot! {:bot-id bot-id}])
                   (.then (fn [_]
                            (reset! status :deployed)
                            (state/nav! :card.type/bot bot-id)))))]
    (let [bot @data]
      [card/wrapper {:variant :wide}
       [card/header {:card card}
        [:<>
         [:span (:bot/name bot)]
         [ui/nav-link {:on-click (fn [_] (state/nav! :game (:game/id (:bot/game bot))))} (str "#" (:game/name (:bot/game bot)))]
         (if (:user/id (:bot/user bot))
           ;; "do later"
           [ui/nav-link {:on-click (fn [_] (state/nav! :user (:user/id (:bot/user bot))))} (str "@" (:name (:user bot)))]
           [ui/nav-link {:on-click (fn [_] (state/log-in!))} "Log in with Github to save your bot"])
         [:div {:tw "grow"}]
         [:div.status
          (case @status
            :picking-language nil
            :editing ""
            :saving "Saving..."
            :saved [ui/nav-button {:on-click (fn [_] (test!))} "TEST"]
            :testing "Testing..."
            :passed [:div {:tw "flex gap-2"}
                     [ui/nav-button {:on-click (fn [_] (test!))} "RE-TEST"]
                     [ui/nav-button {:on-click (fn [_] (deploy!))} "DEPLOY"]]
            :failed [:div {:tw "flex gap-2 items-center"}
                     "Bot error!"
                     [ui/nav-button {:on-click (fn [_] (test!))} "RE-TEST"]]
            :deploying "Deploying..."
            :deployed "Deployed!")]]]
       [card/body {:variant :code}
        (if (nil? (-> bot :bot/code :code/language))
          [:div.lang-pick
           {:tw "p-4"
            :style {:line-height 1.5}}
           [:h2 {:tw "font-bold"} "Pick a language:"]
           (into [:<>]
                 (->> [{:name "Clojure"
                        :language "clojure"}
                       {:name "JavaScript"
                        :language "javascript"}]
                      (map (fn [language]
                             [:a {:tw "block"
                                  :on-click
                                  (fn [_]
                                    (-> (state/tada! [:api/set-bot-language!
                                                      {:bot-id (:bot/id bot)
                                                       :language (:language language)}])
                                        (.then (fn [_data]
                                                 (state/refresh! data)
                                                ;; Relying on the card's auto-refresh to move us to the next state
                                                 ))))}
                              (:name language)]))))]
          [code-editor-view {:on-change (fn [value]
                                          (reset! status :editing)
                                          (debounced-save! value))
                             :language (-> bot :bot/code :code/language)
                             :value (-> bot :bot/code :code/code)}])
        [test-view @test-match bot]]])))
