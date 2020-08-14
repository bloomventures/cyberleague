(ns cyberleague.client.state
  (:require
    [clojure.string :as string]
    [reagent.core :as r]
    [bloom.omni.fx.ajax :as ajax]
    [goog.events :as events]
    [cljs.reader :as reader])
  (:import
    (goog.net XhrIo EventType)))

(defn edn-xhr
  "Send an xhr request with the given data as EDN
  Implementation taken from om-sync."
  [{:xhr/keys [method url data on-complete on-error auth]}]
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

;; state

(defonce state
  (r/atom {:state/cards '()
           :state/user nil}))

(def cards (r/cursor state [:state/cards]))
(def user (r/cursor state [:state/user]))

;; other

(declare fetch-card-data!)

(def card-refresh-rate 1000) ; set to nil to disable

(defn refresh-all-cards!
  []
  (doseq [card @cards]
    (fetch-card-data!
      {:id (card :card/id)
       :type (card :card/type)}
      (fn [data]
        (swap! state update :state/cards
               (fn [cards]
                 (map (fn [*card]
                        (if (= (*card :card/url)
                               (card :card/url))
                          (assoc *card :card/data data)
                          *card))
                      cards)))))))

(defonce update-interval
  (when card-refresh-rate
    (js/setInterval (fn [] (refresh-all-cards!)) card-refresh-rate)))

;; side effect functions

(defn log-out! []
  (edn-xhr {:xhr/url "/api/logout"
            :xhr/method :post
            :xhr/on-complete (fn []
                               (swap! state assoc :state/user nil))}))

(defn log-in! []
  (edn-xhr {:xhr/url "/api/login"
            :xhr/method :post
            :xhr/on-complete (fn [user]
                               (swap! state assoc :state/user user))}))

(defn close-card! [card]
  (swap! state update :state/cards
         (fn [cards] (into [] (remove (fn [c] (= (:card/url c) (:card/url card))) cards)))))

(defn ->url
  [{:keys [id type] :as card}]
  (case type
    :card.type/game (str "/api/games/" id)
    :card.type/games "/api/games"
    :card.type/users "/api/users"
    :card.type/user (str "/api/users/" id)
    :card.type/bot (str "/api/bots/" id)
    :card.type/code (str "/api/bots/" id "/code")
    :card.type/match (str "/api/matches/" id)))

(defn- fetch-card-data!
  [{:keys [id type] :as opts} callback]
  (edn-xhr {:xhr/url (->url opts)
            :xhr/method :get
            :xhr/on-complete callback}))

(defn- open-card!
  [{:keys [type id] :as opts}]
  (let [existing-card (->> @cards
                           (filter (fn [card]
                                     (= (->url opts)
                                        (card :card/url))))
                           first)]
    (if existing-card
      (swap! state update :state/cards
             (fn [cards]
               (-> cards
                   (->> (remove #{existing-card}))
                   (conj existing-card))))
      (fetch-card-data! opts (fn [data]
                               (swap! state update :state/cards conj
                                      {:card/id id
                                       :card/type type
                                       :card/data data
                                       :card/url (->url opts)}))))))

(defn nav!
  [card-type id]
  (open-card! {:type card-type
               :id id}))

(defn fetch-user! []
  (edn-xhr {:xhr/url "/api/user"
            :xhr/method :get
            :xhr/on-complete
            (fn [user]
              (if (user :id)
                (do
                  (swap! state assoc :state/user user)
                  (nav! :card.type/user (user :id)))
                (nav! :card.type/games nil)))}))

(defn bot-set-language! [bot-id language cb]
  (edn-xhr {:xhr/url (str "/api/bots/" bot-id "/language/" language)
            :xhr/method :put
            :xhr/on-complete (fn [data] (cb data))}))

(defn new-bot! [game-id]
  (edn-xhr {:xhr/url (str "/api/games/" game-id "/bot")
            :xhr/method :post
            :xhr/on-complete (fn [data]
                               (nav! :card.type/code (:id data)))}))
