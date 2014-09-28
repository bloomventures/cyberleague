(ns cyberleague.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
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

(defn open-card [card]
  (let [url (condp = (:type card)
              :game (str "/api/games/" (card :id))
              :games "/api/games"
              :chat :chat
              :intro :intro
              :users "/api/users"
              :user (str "/api/users/" (card :id))
              :bot (str "/api/bots/" (card :id))
              :code (str "/api/bots/" (card :id) "/code")
              :match (str "/api/matches/" (card :id)))]
    (if (some #(= url (:url %)) (:cards @app-state))
      (js/console.log "already open")
      (if (= (type "") (type url))
        (edn-xhr {:url url
                  :method :get
                  :on-complete (fn [data]
                                 (swap! app-state (fn [cv] (assoc cv :cards (concat (cv :cards) [(assoc card :data data :url url)])))))})
        (swap! app-state (fn [cv] (assoc cv :cards (concat (cv :cards) [(assoc card :url url)]))))))))

(defn nav [type id]
  (fn [e]
    (open-card {:type type :id id})))

(defn save-code [id code]
  (edn-xhr {:url (str "/api/bots/" id "/code")
            :method :put
            :data {:code code} }))

(defn deploy-bot [id]
  (edn-xhr {:url (str "/api/bots/" id "/deploy")
            :method :post
            :on-complete (nav :bot id)}))


(defn new-bot [game-id]
  (edn-xhr {:url (str "/api/games/" game-id "/bot")
            :method :post
            :on-complete (fn [data] ((nav :code (:id data)))
                                    ((nav :game game-id)))}))
(defn test-bot [bot-id cb]
  (edn-xhr {:url (str "/api/bots/" bot-id "/test")
            :method :post
            :on-complete (fn [match] (cb match)) }))

(def login-csrf-key (atom ""))

(defn log-in []
  (swap! login-csrf-key (fn [cv] (string/join "" (take 20 (repeatedly #(rand-int 9))))))

  (.open js/window
         (str "https://github.com/login/oauth/authorize?client_id=c3e1d987d59e4ab7f433&redirect_uri=http://cyberleague.clojurecup.com/oauth-message&state=" @login-csrf-key)
         "GitHub Auth"
         "width=300,height=400"))

(defn log-out []
  (edn-xhr {:url "/logout"
            :method :post
            :on-complete (fn []
                           (swap! app-state (fn [cv] (assoc cv :user nil))))}))

(defn close [card]
  (fn [e]
    (swap! app-state (fn [cv] (assoc cv :cards (remove (fn [c] (= c card)) (cv :cards)))))))

(defn users-card-view [{:keys [data] :as card} owner]
  (reify
    om/IRender
    (render [_]
      (let [users data]
        (dom/div #js {:className "card users"}
          (dom/header nil "USERS"
                      (dom/a #js {:className "close" :onClick (close card)} "×"))
          (dom/div #js {:className "content"}
            (dom/table nil
              (dom/thead nil
                         (dom/tr nil
                                 (dom/th nil "Name")
                                 (dom/th nil "Active Bots")))
              (apply dom/tbody nil
                (map (fn [user]
                       (dom/tr nil
                               (dom/td nil (dom/a #js {:onClick (nav :user (user :id))} (str "@" (user :name)) ))
                               (dom/td nil (user :bot-count)))) users)))))))))

(defn games-card-view [{:keys [data] :as card} owner]
  (reify
    om/IRender
    (render [_]
      (let [games data]
        (dom/div #js {:className "card games"}
          (dom/header nil "GAMES"
                      (dom/a #js {:className "close" :onClick (close card)} "×"))
          (dom/div #js {:className "content"}
            (dom/table nil
              (dom/thead nil
                         (dom/tr nil
                                 (dom/th nil "Name")
                                 (dom/th nil "Active Bots")))
              (apply dom/tbody nil
                (map (fn [game]
                       (dom/tr nil
                               (dom/td nil (dom/a #js {:onClick (nav :game (game :id))} (str "#" (game :name)) ))
                               (dom/td nil (game :bot-count)))) games)))))))))

(defn game-card-view [{:keys [data] :as card} owner]
  (reify
    om/IRender
    (render [_]
      (let [game data]
        (dom/div #js {:className "card game"}
          (dom/header nil
                      (str "#" (:name game))
                      (when (get-in @app-state [:user :id])
                        (dom/a #js {:className "button" :onClick (fn [e] (new-bot (:id game)))} "CREATE A BOT"))
                      (dom/a #js {:className "close" :onClick (close card)} "×"))
          (dom/div #js {:className "content"}
            (dom/div #js {:dangerouslySetInnerHTML #js {:__html (markdown/md->html (:description game))}})
            (dom/table nil
              (dom/thead nil
                         (dom/tr nil
                                 (dom/th nil "Rank")
                                 (dom/th nil "Bot")
                                 (dom/th nil "Rating")))
              (apply dom/tbody nil
                (map (fn [bot]
                       (dom/tr nil
                               (dom/td nil "#")
                               (dom/td nil
                                       (dom/a #js {:onClick (nav :bot (:id bot))} (:name bot)))
                               (dom/td nil (:rating bot)))) (:bots game))))))))))

(defn bot-card-view [{:keys [data] :as card} owner]
  (reify
    om/IDidMount
    (did-mount [_]
      (js/bot_graph (om/get-node owner "graph") (clj->js (:history data))))

    om/IRender
    (render [_]
      (let [bot data]
        (dom/div #js {:className "card bot"}
          (dom/header nil
                      (dom/span nil (:name bot))
                      (dom/a #js {:className "user-name" :onClick (nav :user (:id (:user bot)))} (str "@" (:name (:user bot))))
                      (dom/a #js {:className "game-name" :onClick (nav :game (:id (:game bot)))} (str "#" (:name (:game bot))))
                      (when (= (get-in @app-state [:user :id]) (:id (:user bot)))
                        (dom/a #js {:className "button" :onClick (nav :code (:id bot))} "CODE"))
                      (dom/a #js {:className "close" :onClick (close card)} "×"))

          (dom/div #js {:className "content"}

            (dom/div #js {:ref "graph"})

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


(defn move-view [move owner]
  (reify
    om/IInitState
    (init-state [_]
      {:log-show false})
    om/IRenderState
    (render-state [_ state]
      (dom/tbody nil
                 (dom/tr #js {:className "clickable" :onClick (fn [e] (om/update-state! owner :log-show not))}
                         (dom/td nil (move "trophy"))
                         (dom/td nil (second (second move)))
                         (dom/td nil (second (last move)))
                         (dom/td nil (if (state :log-show) "×" "▾")))
                 (dom/tr #js {:className (str "log" " " (if (state :log-show) "show" "hide"))}
                         (dom/td #js {:colSpan 4} "console logging TODO"))))))


(defn test-view [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {})
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {:className "test"}
        (when (data :test-match)
          (dom/p nil (cond
                       (:error (data :test-match)) "Your bot had an error."
                       (nil? (:winner (data :test-match))) "Tie!"
                       (= (:id (data :bot)) (:winner (data :test-match))) "You Won!"
                       :else "You Lost!")))
        (when (data :test-match)
          (apply dom/table nil
            (concat [(dom/thead nil
                                (dom/tr nil
                                        (dom/th nil "Trophy")
                                        (dom/th nil "You")
                                        (dom/th nil "Them")
                                        (dom/th nil "")))
                     (dom/tfoot nil
                                (dom/tr nil
                                        (dom/td nil "Score")
                                        (dom/td nil "TODO")
                                        (dom/td nil "TODO")
                                        (dom/td nil nil)))]
                    (om/build-all move-view (:history (data :test-match))))))))))

(defn code-view [bot owner]
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
                (save-code (bot :id) content)
                (recur))))

        (let [cm (js/CodeMirror (om/get-node owner "editor") #js {:value (:code bot)
                                                                  :mode "clojure"
                                                                  :lineNumbers true})]
          (.on cm "changes" (fn [a] (put! update-chan (.getValue cm)))))))

    om/IRender
    (render [_]
      (dom/div #js {:className "source"}
        (dom/div #js {:ref "editor"})))))

(defn code-card-view [{:keys [data] :as card} owner]
  (reify
    om/IInitState
    (init-state [_]
      {:status :saved ; :editing :saved :passing :failing :deployed
       :test-match nil
       })

    om/IRenderState
    (render-state [_ state]
      (let [bot data]
        (dom/div #js {:className "card code"}
          (dom/header nil
                      (dom/span nil (:name bot))
                      (dom/a #js {:onClick (nav :game (:id (:game bot)))} (str "#" (:name (:game bot))))
                      (if (:id (:user bot))
                        (dom/a #js {:onClick (nav :user (:id (:user bot)))} (str "@" (:name (:user bot))))
                        (dom/a #js {:onClick (fn [e] (log-in))} "Log in with Github to save your bot"))
                      (when (= :saved (state :status))
                        (dom/a #js {:className "button test" :onClick (fn [e] (test-bot (:id bot)
                                                                                        (fn [match] (om/set-state! owner :test-match match))))} "TEST"))
                      (when (= :passing (state :status))
                        (dom/a #js {:className "button deploy" :onClick (fn [e] (deploy-bot (:id bot)))} "DEPLOY"))
                      (dom/a #js {:className "close" :onClick (close card)} "×"))
          (dom/div #js {:className "content"}
            (om/build code-view bot)
            (om/build test-view {:test-match (state :test-match)
                                 :bot bot})
            ))))))

(defn match-card-view [{:keys [data] :as card} owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "card match"}
        (dom/header nil "MATCH"
                    (dom/a #js {:className "close" :onClick (close card)} "×"))
        (dom/div #js {:className "content"}
          (str "#" (:name (:game data))))))))

(defn user-card-view [{:keys [data] :as card} owner]
  (reify
    om/IRender
    (render [_]
      (let [user data]
        (dom/div #js {:className "card user"}
          (dom/header nil
                      (dom/span nil (str "@" (:name user)))
                      (dom/a #js {:className "close" :onClick (close card)} "×"))
          (dom/div #js {:className "content"}
            (dom/table nil
              (dom/thead nil
                         (dom/tr nil
                                 (dom/th nil "Game")
                                 (dom/th nil "Bot")
                                 (dom/th nil "Rating")))
              (apply dom/tbody nil
                (map (fn [bot]
                       (dom/tr nil
                               (dom/td nil (:name (:game bot)))
                               (dom/td nil (dom/a #js {:onClick (nav :bot (:id bot))} (:name bot)))
                               (dom/td nil (:rating bot)))) (user :bots))))))))))

(defn chat-card-view [card owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "card chat"}
        (dom/header nil "Chat"
                    (dom/a #js {:className "close" :onClick (close card)} "×"))
        (dom/div #js {:className "content"}
          (dom/iframe #js {:src (str "/chat/" (or (:name (:user @app-state)) "anonymous")) :width "100%" :height "100%"}))))))


(defn intro-card-view [card owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "card intro"}
        (dom/header nil
                    "Welcome to the Cyber League!"
                    (dom/a #js {:className "close" :onClick (close card)} "×"))
        (dom/div #js {:className "content"}
          (dom/p nil "You enjoy playing games. Board games, card games, whatever... you're always up for a challenge. You try to improve your strategy every time you play. However, there just isn't enough time to play out all the possibilities.")
          (dom/p nil "On this site, instead of playing games yourself, you code AI bots to play games for you.")
          (dom/p nil "For now, there's one game (Goofspiel) and one language (ClojureScript).")
          (dom/p nil "You need to log in with Github to create bots.")
          (dom/p nil "Enjoy!")
          (dom/p nil "- Raf & James")
          )))))

(defn app-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "app"}
        (dom/header nil
                    (dom/h1 nil "The Cyber League")
                    (dom/h2 nil "Build AI bots to play games. Best bot wins!")
                    (dom/nav nil
                             (dom/a #js {:className "" :onClick (nav :games nil)} "Games")
                             (dom/a #js {:className "" :onClick (nav :users nil)} "Users")
                             (dom/a #js {:className "" :onClick (nav :chat nil)} "Chat")
                             (when-let [user (data :user)]
                               (dom/a #js {:onClick (nav :user (:id user)) :className "user"}
                                 (dom/img #js {:src (str "https://avatars.githubusercontent.com/u/" (user :gh-id) "?v=2&s=40")})
                                 "My Bots"
                                 ))
                             (if-let [user (data :user)]
                               (dom/a #js {:className "log-out" :onClick (fn [e] (log-out)) :title "Log Out"} "×")
                               (dom/a #js {:className "log-in" :onClick (fn [e] (log-in))} "Log In"))))
        (apply dom/div #js {:className "cards"}
          (map (fn [card]
                 (om/build (condp = (:type card)
                             :game game-card-view
                             :games games-card-view
                             :users users-card-view
                             :chat chat-card-view
                             :intro intro-card-view
                             :bot bot-card-view
                             :code code-card-view
                             :user user-card-view
                             :match match-card-view) card)) (data :cards)))))))

(defn init []
  (om/root app-view app-state {:target (. js/document (getElementById "app"))})

  (js/window.addEventListener "message" (fn [e]
                                          (let [resp (js->clj (.-data e))]
                                            (if (= (resp "state" @login-csrf-key))
                                              (edn-xhr {:url (str "/login/" (resp "code"))
                                                        :method :post
                                                        :on-complete (fn [data]
                                                                       (swap! app-state (fn [cv] (assoc cv :user data)))) })
                                              (js/alert "csrf token error")))))

  (edn-xhr {:url "/api/user"
            :method :get
            :on-complete (fn [data]
                           (if (data :id)
                             (do
                               (swap! app-state (fn [cv] (assoc cv :user data)))
                               (open-card {:type :user :id (data :id)}))
                             (do (doseq [card [{:type :intro :id nil} {:type :games :id nil}]]
                                   (open-card card)))))}))
