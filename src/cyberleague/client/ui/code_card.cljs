(ns cyberleague.client.ui.code-card
  (:require
    [reagent.core :as r]
    [bloom.commons.debounce :refer [debounce]]
    [cyberleague.client.state :as state]
    [cyberleague.client.ui.code-editor :refer [code-editor-view]]
    [cyberleague.client.ui.test :refer [test-view]]))

(defn code-card-view
  [{:keys [card/data] :as card}]
  (let [status (r/atom :saved) ; :saved :editing :saving :testing :passed/:failed :deploying :deployed
        bot-id (:bot/id data)
        test-match (r/atom nil)
        save! (fn [value]
                (reset! status :saving)
                (state/bot-save! bot-id value (fn [_]
                                               (reset! status :saved))))
        debounced-save! (debounce save! 750)
        test! (fn []
                (reset! status :testing)
                (state/bot-test! bot-id (fn [match]
                                          (reset! status (if (nil? (:match/moves match)) :failed :passed))
                                          (reset! test-match match))))
        deploy! (fn []
                  (reset! status :deploying)
                  (state/bot-deploy! bot-id (fn [_]
                                             (reset! status :deployed)
                                             (state/nav! :card.type/bot bot-id))))]
    (fn [{:keys [card/data]}]
      (let [bot data]
        [:div.card.code
        [:header
         [:span (:bot/name bot)]
         [:a {:on-click (fn [_] (state/nav! :game (:game/id (:bot/game bot))))} (str "#" (:game/name (:bot/game bot)))]
         (if (:user/id (:bot/user bot))
           ;; "do later"
           [:a {:on-click (fn [_] (state/nav! :user (:user/id (:bot/user bot))))} (str "@" (:name (:user bot)))]
           [:a {:on-click (fn [_] (state/log-in!))} "Log in with Github to save your bot"])
         [:div.gap]
         [:div.status
          (case @status
            :picking-language nil
            :editing ""
            :saving "Saving..."
            :saved [:a.button.test
                    {:on-click (fn [_] (test!))}
                    "TEST"]
            :testing "Testing..."
            :passed [:<>
                     [:a.button.test
                      {:on-click (fn [_]
                                   (reset! test-match nil)
                                   (test!))}
                      "RE-TEST"]
                     [:a.button.deploy
                      {:on-click (fn [_] (deploy!))}
                      "DEPLOY"]]
            :failed "Bot error!"
            :deploying "Deploying..."
            :deployed "Deployed!")]
         [:a.close {:on-click (fn [_] (state/close-card! card))} "×"]]
        [:div.content
         (if (nil? (-> bot :bot/code :code/language))
           [:div.lang-pick
            [:h2 "Pick a language:"]
            (into [:<>]
                  (->> [{:name "Clojure"
                         :language "clojure"}
                        {:name "JavaScript"
                         :language "javascript"}]
                       (map (fn [language]
                              [:a {:on-click
                                   (fn [_]
                                     (state/bot-set-language!
                                      (:bot/id bot)
                                      (:language language)
                                      (fn [_data]
                                       ;; Relying on the card's auto-refresh to move us to the next state
                                        )))}
                               (:name language)]))))]
           [code-editor-view {:on-change (fn [value]
                                           (reset! status :editing)
                                           (debounced-save! value))
                              :language (-> bot :bot/code :code/language)
                              :value (-> bot :bot/code :code/code)}])
         [test-view @test-match bot]]]))))
