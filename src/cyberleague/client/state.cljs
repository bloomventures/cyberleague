(ns cyberleague.client.state
  (:require
   [clojure.string :as string]
   [reagent.core :as r]
   [goog.events :as events]
   [cljs.reader :as reader]
   [bloom.commons.ajax :as ajax]
   [cyberleague.client.cqrs :as cqrs]
   [cyberleague.client.oauth :as oauth])
  (:import
   (goog.net XhrIo EventType)))

(def tada-events->rest (let [events (cqrs/events)]
                         (zipmap (map :id events)
                                 (map :rest events))))

(defn ajax-promise! [opts]
  (js/Promise.
   (fn [resolve reject]
     (ajax/request (assoc opts
                          :on-success resolve
                          :on-error reject)))))

(defn tada! [[event-id params]]
  (let [[method uri] (tada-events->rest event-id)
        uri (string/replace uri #":([a-z-]+)" (fn [[_ match]] (params (keyword match))))]
    ;; NOTE: Sending all params even though that may sometimes unnecessary,
    ;; but backend just ignores extra params
    (ajax-promise! {:uri uri
                    :method method
                    :params params})))

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
  (-> (ajax-promise! {:uri "/api/logout"
                      :method :post})
      (.then (fn []
               (swap! state assoc :state/user nil)))))

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
  (ajax/request {:uri (->url opts)
                 :method :get
                 :on-success callback}))

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
  (-> (tada! [:api/me])
      (.then (fn [user]
               (if (:user/id user)
                 (do
                   (swap! state assoc :state/user user)
                   (nav! :card.type/user (:user/id user)))
                 (nav! :card.type/games nil))))))

(defn log-in! []
  (oauth/start-auth-flow!
   (fn []
     (fetch-user!))))

(defn bot-set-language! [bot-id language cb]
  (-> (tada! [:api/set-bot-language! {:bot-id bot-id :language language}])
      (.then cb)))

(defn new-bot! [game-id]
  (-> (tada! [:api/create-bot! {:game-id game-id}])
      (.then (fn [data]
               (nav! :card.type/code (:id data))))))

(defn bot-save!
  [bot-id code callback]
  (-> (tada! [:api/set-bot-code! {:bot-id bot-id :code code}])
      (.then callback)))

(defn bot-test!
  [bot-id callback]
  (-> (tada! [:api/test-bot! {:bot-id bot-id}])
      (.then callback)))

(defn bot-deploy!
  [bot-id callback]
  (-> (tada! [:api/deploy-bot! {:bot-id bot-id}])
      (.then callback)))

(defn new-cli-token!
  []
  (-> (tada! [:api/reset-cli-token!])
      (.then (fn [data]
               (swap! user assoc :user/cli-token
                      (:user/cli-token data))))))
