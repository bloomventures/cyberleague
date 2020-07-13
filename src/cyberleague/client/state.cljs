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

;; state

(defonce state
  (r/atom {:cards '()
           :user nil}))

(def cards (r/cursor state [:cards]))
(def user (r/cursor state [:user]))

;; other

(declare fetch-card-data!)

(def card-refresh-rate 1000) ; set to nil to disable

(defn refresh-all-cards!
  []
  (doseq [card @cards]
    (fetch-card-data!
      card
      (fn [data]
        (swap! state update :cards (fn [cards]
                                     (map (fn [*card]
                                            (if (= (*card :url)
                                                   (card :url))
                                              (assoc *card :data data)
                                              *card))
                                          cards)))))))

(defonce update-interval
  (when card-refresh-rate
    (js/setInterval (fn [] (refresh-all-cards!)) card-refresh-rate)))

;; side effect functions

(defn log-out! []
  (edn-xhr {:url "/api/logout"
            :method :post
            :on-complete (fn []
                           (swap! state assoc :user nil))}))

(defn log-in! []
  (edn-xhr {:url "/api/login"
            :method :post
            :on-complete (fn [user]
                           (swap! state assoc :user user))}))

(defn close-card! [card]
  (swap! state update :cards
         (fn [cards] (into [] (remove (fn [c] (= c card)) cards)))))

(defn card->url
  [{:keys [id type] :as card}]
  (case type
    :game (str "/api/games/" id)
    :games "/api/games"
    :users "/api/users"
    :user (str "/api/users/" id)
    :bot (str "/api/bots/" id)
    :code (str "/api/bots/" id "/code")
    :match (str "/api/matches/" id)))

(defn- fetch-card-data!
  [{:keys [id type] :as opts} callback]
  (edn-xhr {:url (card->url opts)
            :method :get
            :on-complete callback}))

(defn- open-card!
  [{:keys [type id] :as opts}]
  (let [existing-card (->> @cards
                           (filter (fn [card]
                                     (= (card->url opts)
                                        (card :url))))
                           first)]
    (if existing-card
      (swap! state update :cards
             (fn [cards]
               (-> cards
                   (->> (remove #{existing-card}))
                   (conj existing-card))))
      (fetch-card-data! opts (fn [data]
                               (swap! state update :cards conj
                                      {:id id
                                       :type type
                                       :data data
                                       :url (card->url opts)}))))))

(defn nav!
  [card-type id]
  (open-card! {:type card-type
               :id id}))

(defn fetch-user! []
  (edn-xhr {:url "/api/user"
            :method :get
            :on-complete
            (fn [user]
              (if (user :id)
                (do
                  (swap! state assoc :user user)
                  (nav! :user (user :id)))
                (do
                  (doseq [card [{:type :games :id nil}]]
                    (nav! (card :type) (card :id))))))}))



(defn bot-set-language! [bot-id language cb]
  (edn-xhr {:url (str "/api/bots/" bot-id "/language/" language)
            :method :put
            :on-complete (fn [data] (cb data))}))

(defn new-bot! [game-id]
  (edn-xhr {:url (str "/api/games/" game-id "/bot")
            :method :post
            :on-complete (fn [data]
                           (nav! :code (:id data)))}))
