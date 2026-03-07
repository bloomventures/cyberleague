(ns cyberleague.client.state
  (:require
   [clojure.edn :as edn]
   [clojure.string :as string]
   [bloom.commons.ajax :as ajax]
   [reagent.core :as r]
   [reagent.ratom :as ratom]
   [cyberleague.client.cqrs :as cqrs]
   [cyberleague.client.oauth :as oauth]
   [goog.crypt.base64 :as base64]))

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

(defn tada-atom
  [[event-id params] & {:keys [refresh-rate]}]
  (let [ratom (ratom/atom nil)
        f (fn []
            (-> (tada! [event-id params])
                (.then (fn [data]
                         (reset! ratom data)))))
        interval (when refresh-rate (js/setInterval f refresh-rate))]
    (f)
    (ratom/make-reaction (fn []
                           (with-meta
                             @ratom
                             {::refresh-fn f}))
                         :on-dispose
                         (fn []
                           (js/clearInterval interval)))))

(defn refresh! [rx]
  ((::refresh-fn (meta @rx))))

;; url state

(defn update-card-url-state! [cards]
  (js/history.replaceState nil "" (str "#" (base64/encodeString (pr-str cards)))))

(defn card-url-state []
  (let [encoded-code (.-hash js/window.location)]
    (when (not (string/blank? encoded-code))
      (edn/read-string (base64/decodeString (.substr encoded-code 1))))))

;; state

(defonce state
  (r/atom {:state/cards (or (card-url-state) '())
           :state/user nil}))

(def cards (r/cursor state [:state/cards]))
(def user (r/cursor state [:state/user]))

(defonce _listener_
  @(r/track!
    (fn []
      (update-card-url-state! @cards))))

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
               (when (empty? @cards)
                 (if (:user/id user)
                   (do
                     (swap! state assoc :state/user user)
                     (nav! :card.type/user (:user/id user)))
                   (nav! :card.type/games nil)))))))

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
