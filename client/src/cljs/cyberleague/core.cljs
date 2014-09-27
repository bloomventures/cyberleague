(ns cyberleague.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <! timeout]]
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

 (defn debounce
  "Given the input channel source and a debouncing time of msecs, return a new
  channel that will forward the latest event from source at most every msecs
  milliseconds"
  [source msecs]
  (let [out (chan)]
    (go
      (loop [state ::init
             lastv nil
             chans [source]]
        (let [[_ threshold] chans]
          (let [[v sc] (alts! chans)]
            (condp = sc
              source (recur ::debouncing v
                            (case state
                              ::init (conj chans (timeout msecs))
                              ::debouncing (conj (pop chans) (timeout msecs))))
              threshold (do (when lastv
                              (put! out lastv))
                            (recur ::init nil (pop chans))))))))
    out))

(def app-state (atom {:cards []}))

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
                             (swap! app-state (fn [cv] (assoc cv :cards (concat (cv :cards) [(assoc card :data data)])))))})))

(defn save-code [id code]
  (edn-xhr {:url (str "/api/bots/" id)
            :method :put
            :data code }))

(defn nav [type id]
  (fn [e]
    (open-card {:type type :id id})))

(defn close [card]
  (fn [e]
    (swap! app-state (fn [cv] (assoc cv :cards (remove (fn [c] (= c card)) (cv :cards)))))))

(defn games-card-view [{:keys [data] :as card} owner]
  (reify
    om/IRender
    (render [_]
      (let [games data]
        (dom/div #js {:className "card games"}
          (dom/header nil "GAMES"
                      (dom/a #js {:className "close" :onClick (close card)} "×"))
          (dom/div #js {:className "content"}
            (apply dom/div nil
              (map (fn [game] (dom/a #js {:onClick (nav :game (game :id))} (game :name) (game :bot-count))) games))))))))

(defn game-card-view [{:keys [data] :as card} owner]
  (reify
    om/IRender
    (render [_]
      (let [game data]
        (dom/div #js {:className "card game"}
          (dom/header nil
                      (:name game)
                      (dom/a #js {:className "button" :onClick (nav :rules (:id game))} "RULES")
                      (dom/a #js {:className "close" :onClick (close card)} "×"))
          (dom/div #js {:className "content"}
            (dom/p nil (:description game))
            (dom/table nil
              (dom/thead nil
                         (dom/tr nil
                                 (dom/th nil "Rank")
                                 (dom/th nil "Bot")
                                 (dom/th nil "Rating")))
              (dom/tbody nil
                         (apply (fn [bot]
                                  (dom/tr nil
                                          (dom/td nil "#")
                                          (dom/td nil
                                                  (dom/a #js {:onClick (nav :bot (:id bot))} (:name bot)))
                                          (dom/td nil (:rating bot)))) (:bots game))))))))))

(defn rules-card-view [{:keys [data] :as card} owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "card rules"}
        (dom/header nil
                    (:name data)
                    " rules"
                    (dom/a #js {:className "close" :onClick (close card)} "×"))
        (dom/div #js {:className "content"}
          (dom/span nil (:name data))
          (dom/span nil (:rules data)))))))

(defn bot-card-view [{:keys [data] :as card} owner]
  (reify
    om/IRender
    (render [_]
      (let [bot data]
        (dom/div #js {:className "card bot"}
          (dom/header nil
                      (dom/div #js {:className "bot"}
                        (dom/span #js {:className "bot-name"} nil (:name bot))
                        (dom/span #js {:className "user-name"} (:name (:user bot)))
                        (dom/a #js {:className "game-name" :onClick (nav :game (:id (:game bot)))} (:name (:game bot))))
                      (dom/a #js {:className "button" :onClick (nav :code (:id bot))} "CODE")
                      (dom/a #js {:className "close" :onClick (close card)} "×"))

          (dom/div #js {:className "content"}
            (dom/div nil "TODO RATING OVER TIME GRAPH")
            (dom/table #js {:className "matches"}
              (dom/thead nil)
              (apply dom/tbody nil
                (map (fn [match]
                       (dom/tr nil
                               (dom/td nil
                                       (dom/a #js {:onClick (nav :match (:id match))}
                                         (if (= (bot :id) (:winner match)) "won" "lost")
                                         " vs "
                                         (let [other-bot (first (remove (fn [b] (= (bot :id) (b :id))) (:bots match)))]
                                           (:name other-bot)))))) (:matches bot))))))))))

(defn code-view [game owner]
  (reify
    om/IInitState
    (init-state [_]
      {:update-chan (chan)})

    om/IDidMount
    (did-mount [_]
      (let [update-chan (om/get-state owner :update-chan)
            debounced-update-chan (debounce update-chan 2000)]
        (go (loop []
              (let [content (<! debounced-update-chan)]
                (save-code (game :id) content)
                (recur))))

        (let [cm (js/CodeMirror (om/get-node owner "code") #js {:value (:code game)
                                                                :mode "clojure"
                                                                :lineNumbers true})]
          (.on cm "changes" (fn [a] (put! update-chan (.getValue cm)))))))

    om/IRender
    (render [_]
      (dom/div #js {:className "code" :ref "code"}))))

(defn code-card-view [{:keys [data] :as card} owner]
  (reify
    om/IRender
    (render [_]
      (let [game data]
        (dom/div #js {:className "card code"}
          (dom/header nil "CODE"
                      (:name game)
                      (dom/a #js {:className "close" :onClick (close card)} "×"))
          (dom/div #js {:className "content"}
            (om/build code-view game)
            ))))))

(defn match-card-view [{:keys [data] :as card} owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "card match"}
        (dom/header nil "MATCH"
                    (dom/a #js {:className "close" :onClick (close card)} "×"))
        (dom/div #js {:className "content"}
          (:name (:game data)))))))

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

(defn init []
  (om/root app-view app-state {:target (. js/document (getElementById "app"))})

  (let [cards [{:type :games}]
        cards [{:type :games}
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
