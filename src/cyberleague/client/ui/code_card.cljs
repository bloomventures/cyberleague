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
        bot (r/atom data)
        test-match (r/atom nil)
        save! (fn [value]
                (reset! status :saving)
                (state/edn-xhr {:xhr/url (str "/api/bots/" bot-id "/code")
                                :xhr/method :put
                                :xhr/data {:bot/code value}
                                :xhr/on-complete (fn [result]
                                                   (reset! status :saved))}))
        debounced-save! (debounce save! 750)
        test! (fn []
                (reset! status :testing)
                (state/edn-xhr
                 {:xhr/url (str "/api/bots/" bot-id "/test")
                  :xhr/method :post
                  :xhr/on-complete (fn [match]
                                     (reset! status (if (:error match) :failed :passed))
                                     (reset! test-match match))}))
        deploy! (fn []
                  (reset! status :deploying)
                  (state/edn-xhr
                   {:xhr/url (str "/api/bots/" bot-id "/deploy")
                    :xhr/method :post
                    :xhr/on-complete (fn [result]
                                       (reset! status :deployed)
                                       (state/nav! :bot :bot/id))}))]
    (fn [_]
      [:div.card.code
       [:header
        [:span (:bot/name @bot)]
        [:a {:on-click (fn [_] (state/nav! :game (:game/id (:bot/game @bot))))} (str "#" (:game/name (:bot/game @bot)))]
        (if (:user/id (:bot/user @bot))
          ;; "do later"
          [:a {:on-click (fn [_] (state/nav! :user (:id (:user @bot))))} (str "@" (:name (:user @bot)))]
          [:a {:on-click (fn [_] (state/log-in!))} "Log in with Github to save your bot"])
        [:div.gap]
        (when (:code/code @bot)
          [:div.status
           (case @status
             :editing ""
             :saving "Saving..."
             :saved [:a.button.test
                     {:on-click (fn [_] (test!))}
                     "TEST"]
             :testing "Testing..."
             :passed [:<>
                      [:a.button.test
                       {:on-click (fn [_] (test!))}
                       "RE-TEST"]
                      [:a.button.deploy
                       {:on-click (fn [_] (deploy!))}
                       "DEPLOY"]]
             :failed "Bot error!"
             :deploying "Deploying..."
             :deployed "Deployed!")])
        [:a.close {:on-click (fn [_] (state/close-card! card))} "×"]]
       [:div.content
        (if (:code/language @bot)
          [code-editor-view {:on-change (fn [value]
                                          (reset! status :editing)
                                          (debounced-save! value))
                             :language (:code/language @bot)
                             :value (:code/code @bot)}]
          [:div.lang-pick
           [:h2 "Pick a language:"]
           (into [:<>]
                 (->> [{:name "Clojure"
                        :language "clojure"}
                       {:name "JavaScript"
                        :language "javascript"}]
                      (map (fn [language]
                             [:a {:on-click (fn [_]
                                              (state/bot-set-language! (:bot/id @bot)
                                                                       (:language language)
                                                                       (fn [data]
                                                                         (swap! bot (fn [b]
                                                                                      (merge b data))))))}

                              (:name language)]))))])
        [test-view @test-match @bot]]])))
