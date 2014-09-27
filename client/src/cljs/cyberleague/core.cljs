(ns cyberleague.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <! sub pub]]
            [clojure.string :as string]
            [goog.events :as events]
            [cljs.reader :as reader])
  (:import [goog.net XhrIo EventType]))

(enable-console-print!)

(defn edn-xhr
  "Send an xhr request with the given data as EDN
  Implementation taken from om-sync."
  [{:keys [method url data on-complete on-error auth]}]
  (let [xhr (XhrIo.)]
    (when on-complete
      (events/listen xhr EventType.SUCCESS
        (fn [e] (on-complete (reader/read-string (.getResponseText xhr))))))
    (when on-error
      (events/listen xhr EventType.ERROR
        (fn [e] (on-error {:error (.getResponseText xhr)}))))
    (.send xhr url (.toUpperCase (name method)) (when data (pr-str data))
           #js {"Content-Type" "application/edn"
                "Accept" "application/edn"
                "Authorization" (str "Basic " (js/btoa (string/join ":" auth)))})))

(def app-state (atom {:cards []}))

(defn games-card-view [{:keys [data] :as card} owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "card"}
        (dom/header nil "GAMES")
        (apply dom/div nil
          (map (fn [game] (dom/a nil (game :name) (game :bot-count))) (data :games)))))))

(defn game-card-view [{:keys [data] :as card} owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "card"}
        (dom/header nil "GAME")
        (:name data)
        (:description data)
        (string/join " " (map (fn [bot] (:name bot)) (:bots data)))))))

(defn rules-card-view [{:keys [data] :as card} owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "card"}
        (dom/header nil "RULES")
        (dom/span nil (:name data))
        (dom/span nil (:rules data))))))

(defn bot-card-view [{:keys [data] :as card} owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "card"}
        (dom/header nil "BOT")
        (:name data)
        (:name (:user data))
      (:name (:game data))
      "todo history"
      "todo matches"))))

(defn code-card-view [{:keys [data] :as card} owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "card"}
        (dom/header nil "CODE")
        (:name data)
        (:code data)))))

(defn match-card-view [{:keys [data] :as card} owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "card"}
        (dom/header nil "MATCH")
        (:name (:game data))))))

(defn app-view [data owner]
  (reify
    om/IRender
    (render [_]
      (apply dom/div #js {:className "cards"}
        (map (fn [card]
               (om/build (condp = (:type card)
                           :game game-card-view
                           :games games-card-view
                           :rules rules-card-view
                           :bot bot-card-view
                           :code code-card-view
                           :match match-card-view) card)) (data :cards))))))

(defn open-card [card]
  (let [url (condp = (:type card)
              :game (str "/api/games/" (card :id))
              :games "/api/games"
              :rules (str "/api/games/" (card :id) "/rules")
              :bot (str "/api/bots/" (card :id))
              :code (str "/api/bots/" (card :id) "/code")
              :match (str "/api/matches/" (card :id)))]

    (edn-xhr {:url url
              :method :get
              :on-complete (fn [data]
                             (swap! app-state (fn [cv] (assoc cv :cards (conj (cv :cards) (assoc card :data data))))))})))


(defn init []
  (om/root app-view app-state {:target (. js/document (getElementById "app"))})

  (let [cards [{:type :games}
               {:type :game
                :id 123}
               {:type :rules
                :id 123}
               {:type :bot
                :id 345}
               {:type :code
                :id 345}
               {:type :match
                :id 456}]]
    (doseq [card cards]
      (open-card card))))
