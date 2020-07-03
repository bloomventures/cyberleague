(ns cyberleague.client.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent defcomponentmethod]]
            [cljs.core.async :refer [put! chan <! timeout]]
            [markdown.core :as markdown]
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
(def nav-chan (chan))

(defn nav [type id]
  (fn [e]
    (put! nav-chan [:open {:type type :id id}])))


(defn bot-set-language [bot-id language cb]
  (edn-xhr {:url (str "/api/bots/" bot-id "/language/" language)
            :method :put
            :on-complete (fn [data] (cb data))}))

(defn new-bot [game-id]
  (edn-xhr {:url (str "/api/games/" game-id "/bot")
            :method :post
            :on-complete (fn [data] ((nav :code (:id data))))}))

(def login-csrf-key (atom ""))

(defn log-in []
  (swap! login-csrf-key (fn [cv] (string/join "" (take 20 (repeatedly #(rand-int 9))))))

  (.open js/window
         (str "https://github.com/login/oauth/authorize?client_id=" js/window.github_app_id "&redirect_uri=" js/window.github_redirect_uri "&state=" @login-csrf-key)
         "GitHub Auth"
         "width=300,height=400"))

(defn log-out []
  (edn-xhr {:url "/logout"
            :method :post
            :on-complete (fn []
                           (swap! app-state (fn [cv] (assoc cv :user nil))))}))

(defn close [card]
  (fn [e]
    (put! nav-chan [:close card])))


(defmulti card-view (fn [card owner opts] (card :type)))

(defcomponentmethod card-view :users
  [{:keys [data] :as card} owner opts]
  (render [_]
    (let [users data]
      (dom/div {:class "card users"}
        (dom/header "USERS"
          (dom/a {:class "close" :on-click (close card)} "×"))
        (dom/div {:class "content"}
          (dom/table
            (dom/thead
              (dom/tr nil
                (dom/th "Name")
                (dom/th "Active Bots")))
            (dom/tbody nil
              (map (fn [user]
                     (dom/tr nil
                       (dom/td (dom/a {:on-click (nav :user (user :id))} (str "@" (user :name)) ))
                       (dom/td (user :bot-count)))) users))))))))

(defcomponentmethod card-view :games
  [{:keys [data] :as card} owner]
  (render [_]
    (let [games data]
      (dom/div {:class "card games"}
        (dom/header "GAMES"
          (dom/a {:class "close" :on-click (close card)} "×"))
        (dom/div {:class "content"}
          (dom/table nil
            (dom/thead
              (dom/tr nil
                (dom/th "Name")
                (dom/th "Active Bots")))
            (dom/tbody nil
              (map (fn [game]
                     (dom/tr nil
                       (dom/td
                         (dom/a {:on-click (nav :game (game :id))}
                           (str "#" (game :name)) ))
                       (dom/td (game :bot-count)))) games))))))))

(defcomponentmethod card-view :game
  [{:keys [data] :as card} owner]
  (render [_]
    (let [game data]
      (dom/div {:class "card game"}
        (dom/header nil
          (str "#" (:name game))
          (when (get-in @app-state [:user :id])
            (dom/a {:class "button" :on-click (fn [e] (new-bot (:id @game)))} "CREATE A BOT"))
          (dom/a {:class "close" :on-click (close card)} "×"))
        (dom/div {:class "content"}
          (dom/div {:dangerouslySetInnerHTML #js {:__html (markdown/md->html (:description game))}})
          (dom/table nil
            (dom/thead
              (dom/tr nil
                (dom/th "Rank")
                (dom/th "Bot")
                (dom/th "Rating")))
            (dom/tbody
              (->> (:bots game)
                   (sort-by :rating)
                   reverse
                   (map-indexed (fn [rank bot]
                                  (dom/tr nil
                                    (dom/td rank)
                                    (dom/td
                                      (dom/a {:on-click (nav :bot (:id bot))}
                                        (if (= :active (:status bot)) "●" "○") " "
                                        (:name bot)))
                                    (dom/td (:rating bot)))))))))))))

(defcomponentmethod card-view :bot
  [{:keys [data] :as card} owner]
  (did-mount [_]
    (js/bot_graph (om/get-node owner "graph") (clj->js (:history data))))

  (render [_]
    (let [bot data]
      (dom/div {:class "card bot"}
        (dom/header nil
          (dom/span (:name bot))
          (dom/a {:class "user-name"
                  :on-click (nav :user (:id (:user bot)))}
            (str "@" (:name (:user bot))))
          (dom/a {:class "game-name"
                  :on-click (nav :game (:id (:game bot)))}
            (str "#" (:name (:game bot))))
          (when (= (get-in @app-state [:user :id]) (:id (:user bot)))
            (dom/a {:class "button" :on-click (nav :code (:id bot))} "CODE"))
          (dom/a {:class "close" :on-click (close card)} "×"))

        (dom/div {:class "content"}

          (dom/div {:ref "graph"})

          (dom/table {:class "matches"}
            (dom/thead nil)
            (dom/tbody nil
              (map (fn [match]
                     (dom/tr
                       (dom/td
                         (dom/a {:on-click (nav :match (:id match))}
                           (condp = (:winner match)
                             nil "tied"
                             (bot :id) "won"
                             "lost")
                           " vs "
                           (let [other-bot (first (remove (fn [b] (= (bot :id) (b :id))) (:bots match)))]
                             (:name other-bot))))))
                   (:matches bot)))))))))


(defmulti match-results (comp :name :game))

(defcomponentmethod match-results :default
  [match owner opts]
  (render [_]
    (println match)
    (dom/div (str "Moves: " (:moves match)))))

(defcomponent move-view [move owner {:keys [p1-id p2-id] :as opts}]
  (init-state [_]
    {:log-show false})
  (render-state [_ state]
    (dom/tbody nil
      (dom/tr {:class "clickable" :on-click (fn [e] (om/update-state! owner :log-show not))}
        (dom/td (move "trophy"))
        (dom/td {:class (when (> (move p1-id) (move p2-id)) "winner")} (move p1-id))
        (dom/td {:class (when (< (move p1-id) (move p2-id)) "winner")} (move p2-id))
        (dom/td (if (state :log-show) "×" "▾")))
      (dom/tr {:class (str "log" " " (if (state :log-show) "show" "hide"))}
        (dom/td {:colSpan 4} "console logging TODO")))))

(defcomponentmethod match-results "goofspiel"
  [{:keys [bots winner] :as match} owner opts]
  (render [_]
    (let [p1-id (:id (first bots))
          p2-id (:id (second bots))]
      (dom/table {:class "results goofspiel"}
        (concat
          [(dom/thead nil
             (dom/tr nil
               (dom/th "Trophy")
               (dom/th (:name (first bots)))
               (dom/th (:name (second bots)))
               (dom/th "")))
           (dom/tfoot
             (dom/tr nil
               (dom/td "Score")
               (dom/td {:class (when (= p1-id winner) "winner")}
                 (->> match
                      :moves
                      (map (fn [move] (if (> (move p1-id) (move p2-id)) (move "trophy") 0)))
                      (apply +)))
               (dom/td {:class (when (= p2-id winner) "winner")}
                 (->> match
                      :moves
                      (map (fn [move] (if (< (move p1-id) (move p2-id)) (move "trophy") 0)))
                      (apply +)))
               (dom/td nil)))]
          (om/build-all move-view (:moves match) {:opts {:p1-id p1-id :p2-id p2-id}}))))))

(defcomponentmethod match-results "ultimate tic-tac-toe"
  [match owner opts]
  (init-state [_]
    {:current-move (count (match :moves))})
  (render-state [_ {:keys [current-move]}]
    (let [moves (match :moves)
            displayed-moves (take current-move moves)
            p1 (get (first moves) "player")
            p2 (get (second moves) "player")
            [p1-moves p2-moves] (->> displayed-moves
                                     (map #(get % "move"))
                                     (partition 2 2 nil)
                                     ((juxt (partial map first) (partial map second)))
                                     (map set))]
      (dom/div nil
        (dom/table {:class "results tic-tac-toe"}
          (dom/thead)
          (dom/tbody nil
            (for [row (partition 3 (range 9))]
              (dom/tr nil
                (map (fn [board-idx]
                       (dom/td nil
                         (dom/table {:class "subboard"}
                           (dom/thead)
                           (dom/tbody nil
                             (for [sub-row (partition 3 (range 9))]
                               (dom/tr nil
                                 (map (fn [subboard-idx]
                                        (let [winner (condp contains? [board-idx subboard-idx]
                                                       p1-moves :p1
                                                       p2-moves :p2
                                                       :no)]
                                          (dom/td {:class (name winner)}
                                            (case winner
                                              :p1 "X"
                                              :p2 "O"
                                              :no "."))))
                                      sub-row))
                               )))))
                     row)))))
        (dom/div nil
          (str "Turn " current-move "/" (count moves))
          (dom/br)
          (dom/input {:type "range"
                      :min 0
                      :max (count moves)
                      :step 1
                      :value current-move
                      :on-change (fn [e] (om/set-state! owner :current-move (.. e -target -value)))}
            ()))))))

(defcomponentmethod card-view :match
  [{:keys [data] :as card} owner]
  (render [_]
    (dom/div {:class "card match"}
      (dom/header "MATCH"
        (dom/a {:on-click (nav :game (:id (:game data)))} (str "#" (:name (:game data))))
        (dom/a {:class "close" :on-click (close card)} "×"))
      (dom/div {:class "content"}

        (dom/h1
          (dom/a {:on-click (nav :bot (:id (first (:bots data))))} (:name (first (:bots data))))
          " vs "
          (dom/a {:on-click (nav :bot (:id (second (:bots data))))} (:name (second (:bots data)))))

        (dom/div {:class "moves"}
          (om/build match-results data))))))

(defcomponent test-view [data owner]
  (init-state [_]
    {})
  (render-state [_ state]
    (let [bot-id (:id (data :bot))]
      (dom/div {:class "test"}
        (when (data :test-match)
          (dom/p
            (cond
              (:error (data :test-match)) (:info (data :test-match))
              (nil? (:winner (data :test-match))) "Tie!"
              (= (:id (data :bot)) (:winner (data :test-match))) "You Won!"
              :else "You Lost!")))
        (when (data :test-match)
          (om/build match-results (data :test-match)))))))


(defcomponent code-editor-view [{:keys [bot action-chan]} owner]
  (did-mount [_]
    (let [cm (js/CodeMirror (om/get-node owner "editor") #js {:value (:code bot)
                                                              :mode (case (:language bot)
                                                                      "clojurescript" "clojure"
                                                                      "javascript" "javascript")
                                                              :lineNumbers true})]
      (.on cm "changes" (fn [a] (put! action-chan [:type (.getValue cm)])))))

  (render [_]
    (dom/div {:class "source"}
      (dom/div {:ref "editor"}))))

(defcomponentmethod card-view :code
  [{:keys [data] :as card} owner]
  (init-state [_]
    {:status :saved ; :saved :editing :saving :testing :passed/:failed :deploying :deployed
     :action-chan (chan)
     :test-match nil
     })

  (will-mount [_]
    (let [action-chan (om/get-state owner :action-chan)
          type-chan (chan)
          debounced-type-chan (debounce type-chan 750)
          bot-id (data :id)]

      (go (loop []
            (let [content (<! debounced-type-chan)]
              (put! action-chan [:stopped-typing content])
              (recur))))

      (go (loop []
            (let [[action content] (<! action-chan)]
              (case action
                :type
                (do (om/set-state! owner :status :editing)
                    (put! type-chan content))

                :stopped-typing
                (do (om/set-state! owner :status :saving)
                    (edn-xhr {:url (str "/api/bots/" bot-id "/code")
                              :method :put
                              :data {:code content}
                              :on-complete (fn [result] (put! action-chan [:save-result result]))}))

                :save-result
                (do (om/set-state! owner :status :saved))

                :test
                (do (om/set-state! owner :status :testing)
                    (edn-xhr {:url (str "/api/bots/" bot-id "/test")
                              :method :post
                              :on-complete (fn [match] (put! action-chan [:test-result match]))}))

                :test-result
                (do (om/set-state! owner :status (if (content :error) :failed :passed))
                    (om/set-state! owner :test-match content))

                :deploy
                (do (om/set-state! owner :status :deploying)
                    (edn-xhr {:url (str "/api/bots/" bot-id "/deploy")
                              :method :post
                              :on-complete (fn [result] (put! action-chan [:deploy-result result]))}))

                :deploy-result
                (do (om/set-state! owner :status :deployed)
                    ((nav :bot bot-id))))

              (recur))))))

  (render-state [_ state]
    (let [bot data]
      (dom/div {:class "card code"}
        (dom/header nil
          (dom/span (:name bot))
          (dom/a {:on-click (nav :game (:id (:game bot)))} (str "#" (:name (:game bot))))
          (if (:id (:user bot))
            (dom/a {:on-click (nav :user (:id (:user bot)))} (str "@" (:name (:user bot))))
            (dom/a {:on-click (fn [e] (log-in))} "Log in with Github to save your bot"))
          (when (bot :code)
            (dom/div {:class "status"}
              (case (state :status)
                :editing ""
                :saving "Saving..."
                :saved (dom/a {:class "button test"
                               :on-click (fn [e] (put! (state :action-chan) [:test]))} "TEST")
                :testing "Testing..."
                :passed (dom/a {:class "button deploy" :on-click (fn [e] (put! (state :action-chan) [:deploy]) )} "DEPLOY")
                :failed "Bot error!"
                :deploying "Deploying..."
                :deployed "Deployed!")))
          (dom/a {:class "close" :on-click (close card)} "×"))
        (dom/div {:class "content"}
          (if (bot :language)
            (om/build code-editor-view {:bot bot :action-chan (state :action-chan) })
            (dom/div {:class "lang-pick"}
              (dom/h2 "Pick a language:")
              (map (fn [language]
                     (dom/a {:on-click (fn [e] (bot-set-language (:id @bot)
                                                                 (:language language)
                                                                 (fn [data]
                                                                   (om/transact! bot (fn [b] (merge b data))))))}
                       (:name language)))
                   [{:name "ClojureScript"
                     :language "clojurescript"}
                    {:name "JavaScript"
                     :language "javascript"}])))
          (om/build test-view {:test-match (state :test-match)
                               :bot bot}))))))


(defcomponentmethod card-view :user
  [{:keys [data] :as card} owner]

  (render [_]
    (let [user data]
      (dom/div {:class "card user"}
        (dom/header nil
          (dom/span (str "@" (:name user)))
          (dom/a {:class "close" :on-click (close card)} "×"))
        (dom/div {:class "content"}
          (dom/table nil
            (dom/thead
              (dom/tr nil
                (dom/th "Game")
                (dom/th "Bot")
                (dom/th "Rating")))
            (dom/tbody
              (map (fn [bot]
                     (dom/tr nil
                       (dom/td
                         (dom/a {:on-click (nav :game (:id (:game bot)))}
                           (str "#" (:name (:game bot)))))
                       (dom/td
                         (dom/a {:on-click (nav :bot (:id bot))}
                           (if (= :active (:status bot)) "●" "○") " "
                           (:name bot)))
                       (dom/td (:rating bot))))
                   (user :bots)))))))))

(defcomponentmethod card-view :chat
  [card owner]
  (render [_]
    (dom/div {:class "card chat"}
      (dom/header nil "Chat"
        (dom/a {:class "close" :on-click (close card)} "×"))
      (dom/div {:class "content"}
        (dom/iframe {:src (str "/chat/" (or (:name (:user @app-state)) "anonymous"))
                     :width "100%"
                     :height "100%"})))))

(defcomponent app-view [data owner]
  (will-mount [_]
    (go (loop []
          (let [[action card] (<! nav-chan)]
            (case action
              :close (om/transact! data :cards (fn [cards] (into [] (remove (fn [c] (= c @card)) cards))))
              :open (let [url (condp = (:type card)
                                :game (str "/api/games/" (card :id))
                                :games "/api/games"
                                :chat :chat
                                :users "/api/users"
                                :user (str "/api/users/" (card :id))
                                :bot (str "/api/bots/" (card :id))
                                :code (str "/api/bots/" (card :id) "/code")
                                :match (str "/api/matches/" (card :id)))]
                      (if (some #(= url (:url %)) (:cards @data))
                        (js/console.log "already open")
                        (if (string? url)
                          (edn-xhr {:url url
                                    :method :get
                                    :on-complete
                                    (fn [card-data]
                                      (om/transact! data :cards (fn [cards] (conj cards (assoc card
                                                                                          :data card-data
                                                                                          :url url)))))})
                          (om/transact! data :cards (fn [cards] (conj cards (assoc card :url url))))))))
            (recur))))

    (edn-xhr {:url "/api/user"
              :method :get
              :on-complete
              (fn [user]
                (if (user :id)
                  (do
                    (swap! app-state assoc :user user)
                    ((nav :user (user :id))))
                  (do (doseq [card [{:type :games :id nil}]]
                        ((nav (card :type) (card :id)))))))}))

  (render [_]
    (dom/div {:class "app"}
      (dom/header nil
        (dom/h1 "The Cyber League")
        (dom/h2 "Code bots to play games.")
        (dom/nav nil
          (dom/a {:class "" :on-click (nav :games nil)} "Games")
          (dom/a {:class "" :on-click (nav :users nil)} "Users")
          (dom/a {:class "" :on-click (nav :chat nil)} "Chat")
          (when-let [user (data :user)]
            (dom/a {:on-click (nav :user (:id user)) :class "user"}
              (dom/img {:src (str "https://avatars.githubusercontent.com/u/" (user :gh-id) "?v=2&s=40")})
              "My Bots"))
          (if-let [user (data :user)]
            (dom/a {:class "log-out" :on-click (fn [e] (log-out)) :title "Log Out"} "×")
            (dom/a {:class "log-in" :on-click (fn [e] (log-in))} "Log In"))))
      (dom/div {:class "cards"}
        (om/build-all card-view (data :cards) {:key :url})))))

(defn ^:export init []
  (om/root app-view app-state {:target (. js/document (getElementById "app"))})
  (js/window.addEventListener
    "message"
    (fn [e]
      (let [resp (js->clj (.-data e))]
        (if (= (resp "state" @login-csrf-key))
          (edn-xhr {:url (str "/login/" (resp "code"))
                    :method :post
                    :on-complete (fn [data]
                                   (swap! app-state assoc :user data))})
          (js/alert "csrf token error"))))))