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
    on-code-change! (fn [value]
                      (reset! status :editing)
                      (debounced-save! value))
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
      [card/header {:card card
                    :refresh [data]}
       [:<>
        [:span [ui/bot-chip bot]]
        [ui/nav-link {:on-click (fn [_] (state/nav! :game (:game/id (:bot/game bot))))} (str "#" (:game/name (:bot/game bot)))]
        (if (:user/id (:bot/user bot))
          ;; "do later"
          [ui/nav-link {:on-click (fn [_] (state/nav! :user (:user/id (:bot/user bot))))} (str "@" (:name (:user bot)))]
          [ui/nav-link {:on-click (fn [_] (state/log-in!))} "Log in with Github to save your bot"])
        [:div {:tw "grow"}]
        [:div.status
         (case @status
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
       (when bot
         [code-editor-view {;; don't inline a fn here, b/c it breaks editing
                            ;; if inlined, when parent component re-renders (often)
                            ;; it passes a new fn to this sub-component,
                            ;; re-rendering with the *old* code value
                            :on-change on-code-change!
                            :language (-> bot :bot/code :code/env :env/language :language/slug)
                            :value (-> bot :bot/code :code/code)}])
       [test-view @test-match bot]]])))
