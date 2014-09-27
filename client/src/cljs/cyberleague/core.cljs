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

(def app-state (atom {:cards [{:type :games}
                              {:type :game
                               :id 123}
                              {:type :rules
                               :id 123}
                              {:type :bot
                               :id 345}
                              {:type :code
                               :id 345}
                              {:type :match
                               :id 456}]}))

(defn games-card-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "card"}
        (apply dom/div nil
          (map (fn [game] (dom/a nil (game :name) (game :bot-count))) (data :games)))))))

(defn game-card-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "card"} "GAME")
      )))

(defn rules-card-view [data owner]
  (reify
    om/IRender
    (render [_]

      (dom/div #js {:className "card"} "RULES")
      )))

(defn bot-card-view [data owner]
  (reify
    om/IRender
    (render [_]

      (dom/div #js {:className "card"} "BOT")
      )))

(defn code-card-view [data owner]
  (reify
    om/IRender
    (render [_]

      (dom/div #js {:className "card"} "CODE")
      )))

(defn match-card-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "card"} "MATCH")
      )))

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
                           :match match-card-view) data)) (data :cards))))))

(defn init []
  (om/root app-view app-state {:target (. js/document (getElementById "app"))})

  (edn-xhr {:url "/api/games"
            :method :get
            :on-complete (fn [data]
                           (swap! app-state assoc :games (:games data)))})
  )
