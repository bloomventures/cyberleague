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

(defonce state
  (r/atom {:cards []
           :user nil}))

(def cards (r/cursor state [:cards]))
(def user (r/cursor state [:user]))

(defn log-out! []
  (edn-xhr {:url "/api/logout"
            :method :post
            :on-complete (fn []
                           (swap! state assoc :user nil))}))

(defn log-in! []
  (edn-xhr {:url "/api/login"
            :method :post
            :on-complete (fn [data]
                           (swap! state assoc :user data))}))

(defn close-card! [card]
  (swap! state update :cards
         (fn [cards] (into [] (remove (fn [c] (= c card)) cards)))))

(defn- open-card! [card]
  (let [url (case (:type card)
              :game (str "/api/games/" (card :id))
              :games "/api/games"
              :users "/api/users"
              :user (str "/api/users/" (card :id))
              :bot (str "/api/bots/" (card :id))
              :code (str "/api/bots/" (card :id) "/code")
              :match (str "/api/matches/" (card :id)))]
    (if (some (fn [card] (= url (:url card)))
              @cards)
      (do
        ;; nothing
        )
      (edn-xhr {:url url
                :method :get
                :on-complete
                (fn [data]
                  (swap! state update :cards conj
                         (assoc card
                           :data data
                           :url url)))}))))

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
