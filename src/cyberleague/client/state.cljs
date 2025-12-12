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

(defn tada-atom [[event-id params] & {:keys [refresh]}]
  (let [refresh-fn (atom nil)
        a (with-meta (r/atom nil) {::refresh-fn refresh-fn})
        f (fn []
            (-> (tada! [event-id params])
                (.then (fn [data]
                         (reset! a data)))))]
    (reset! refresh-fn f)
    (f)
    (when refresh (js/setInterval f refresh))
    a))

(defn refresh! [a]
  (@(::refresh-fn (meta a))))

;; state

(defonce state
  (r/atom {:state/cards '()
           :state/user nil}))

(def cards (r/cursor state [:state/cards]))
(def user (r/cursor state [:state/user]))

;; side effect functions

(defn log-out! []
  (-> (ajax-promise! {:uri "/api/logout"
                      :method :post})
      (.then (fn []
               (swap! state assoc :state/user nil)))))

(defn close-card! [card]
  (swap! state update :state/cards
         (fn [cards] (remove #{card} cards))))

(defn- open-card!
  [card]
  (swap! state update :state/cards
         (fn [cards]
           (-> cards
               (->> (remove #{card}))
               (conj card)))))

(defn nav!
  [card-type id]
  (open-card! [card-type {:id id}]))

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

(defn new-cli-token!
  []
  (-> (tada! [:api/reset-cli-token!])
      (.then (fn [data]
               (swap! user assoc :user/cli-token
                      (:user/cli-token data))))))
