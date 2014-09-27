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
              :chat :chat
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
  (edn-xhr {:url (str "/api/bots/" id)
            :method :put
            :data code }))

(defn deploy-bot [id]
  (edn-xhr {:url (str "/api/bots/" id "/deploy")
            :method :post
            :on-complete (nav :bot id)}))


(defn new-bot [game-id]
  (edn-xhr {:url (str "/api/bots")
            :method :post
            :data {:game-id game-id}
            :on-complete (fn [data] ((nav :code (:id data)))
                                    ((nav :game game-id)))}))

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
                      (dom/a #js {:className "button" :onClick (fn [e] (new-bot (:id game)))} "NEW BOT")
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

(defn bot-card-view [{:keys [data] :as card} owner]
  (reify
    om/IRender
    (render [_]
      (let [bot data]
        (dom/div #js {:className "card bot"}
          (dom/header nil
                      (:name bot)
                      (dom/a #js {:className "button" :onClick (nav :code (:id bot))} "CODE")
                      (dom/a #js {:className "close" :onClick (close card)} "×"))

          (dom/div #js {:className "content"}
            (dom/a #js {:className "user-name" :onClick (nav :user (:id (:user bot)))} (:name (:user bot)))
            (dom/a #js {:className "game-name" :onClick (nav :game (:id (:game bot)))} (:name (:game bot)))
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


(defn move-view [move owner]
  (reify
    om/IInitState
    (init-state [_]
      {:log-show false})
    om/IRenderState
    (render-state [_ state]
      (dom/tbody nil
                 (dom/tr #js {:className "clickable" :onClick (fn [e] (om/update-state! owner :log-show not))}
                         (dom/td nil move)
                         (dom/td nil move)
                         (dom/td nil move)
                         (dom/td nil move)
                         (dom/td nil move)
                         (dom/td nil (if (state :log-show) "▴" "▾")))
                 (dom/tr #js {:className (str "log" " " (if (state :log-show) "show" "hide"))}
                         (dom/td #js {:colSpan 6} "console logs"))))))


(defn test-view [bot owner]
  (reify
    om/IInitState
    (init-state [_]
      {})
    om/IRenderState
    (render-state [_ state]
      (dom/div #js {:className "test"}

        (apply dom/table nil
          (concat [(dom/thead nil
                              (dom/tr nil
                                      (dom/th nil "Trophy")
                                      (dom/th nil "Your Move")
                                      (dom/th nil "Your Score")
                                      (dom/th nil "Their Move")
                                      (dom/th nil "Their Score")
                                      (dom/th nil "")))
                   (dom/tfoot nil
                              (dom/tr nil
                                      (dom/th nil  "Final")
                                      (dom/th nil nil)
                                      (dom/th nil 5)
                                      (dom/th nil nil)
                                      (dom/th nil 6)
                                      (dom/th nil nil)))]
                  (om/build-all move-view [1 2 3 4 5])))
        "you win!"))))

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
    om/IRender
    (render [_]
      (let [bot data]
        (dom/div #js {:className "card code"}
          (dom/header nil "CODE"
                      (:name bot)
                      (dom/a #js {:className "button rules" :onClick (nav :game (:id (:game bot)))} "RULES")
                      (dom/a #js {:className "button test"} "TEST")
                      (dom/a #js {:className "button deploy" :onClick (fn [e] (deploy-bot (:id bot)))} "DEPLOY")
                      (dom/a #js {:className "close" :onClick (close card)} "×"))
          (dom/div #js {:className "content"}
            (om/build code-view bot)
            (om/build test-view bot)
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

(defn user-card-view [{:keys [data] :as card} owner]
  (reify
    om/IRender
    (render [_]
      (let [user data]
        (dom/div #js {:className "card user"}
          (dom/header nil "USER"
                      (dom/span nil (:name user))
                      (dom/a #js {:className "close" :onClick (close card)} "×"))
          (dom/div #js {:className "content"}
            (apply dom/div nil
              (map (fn [bot]
                     (dom/a #js {:onClick (nav :bot (:id bot))} (:name (:game bot)) "/" (:name bot))) (user :bots)))))))))

(def login-csrf-key (atom ""))

(defn log-in []
  (swap! login-csrf-key (fn [cv] (string/join "" (take 20 (repeatedly #(rand-int 9))))))

  (.open js/window
         (str "https://github.com/login/oauth/authorize?client_id=***REMOVED***&redirect_uri=http://cyberleague.clojurecup.com/oauth-message&state=" @login-csrf-key)
         "GitHub Auth"
         "width=300,height=400"))

(defn log-out []
  (edn-xhr {:url "/logout"
            :method :post
            :on-complete (fn []
                           (swap! app-state (fn [cv] (assoc cv :user nil))))}))

(defn chat-card-view [card owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "card chat"}
        (dom/header nil "Report Bugs"
                    (dom/a #js {:className "close" :onClick (close card)} "×"))
        (dom/div #js {:className "content"}
          (dom/iframe #js {:src "http://www.hipchat.com/g5EB5iQjr?anonymous=1&minimal=1&timezone=CDT" :width "100%" :height "100%"}))))))

(defn app-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "app"}
        (dom/header nil
                    (dom/h1 nil "The Cyber League")
                    (dom/h2 nil "Build AI bots to play games. Best bot wins!")
                    (dom/nav nil
                             (dom/a #js {:className "button" :onClick (nav :games nil)} "All Games")
                             (dom/a #js {:className "button" :onClick (nav :chat nil)} "Report Bugs")
                             (when-let [user (data :user)]
                               (dom/a #js {:onClick (nav :user (:id user)) :className "user button"}
                                 (dom/img #js {:src (str "https://avatars.githubusercontent.com/u/" (user :id) "?v=2&s=40")})
                                 "My Bots"))
                             (if-let [user (data :user)]
                               (dom/a #js {:className "button log-out" :onClick (fn [e] (log-out))}  "Log Out")
                               (dom/a #js {:className "button" :onClick (fn [e] (log-in))} "Log In"))))
        (apply dom/div #js {:className "cards"}
          (map (fn [card]
                 (om/build (condp = (:type card)
                             :game game-card-view
                             :games games-card-view
                             :chat chat-card-view
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
                           (when (data :id)
                             (swap! app-state (fn [cv] (assoc cv :user data))))
                           )})

  (let [cards [{:type :code
                :id 345}]
        _cards [{:type :games}
               {:type :game
                :id 123}
               {:type :bot
                :id 345}
               {:type :code
                :id 345}
               {:type :match
                :id 456}]]
    (doseq [card cards]
      (open-card card))))
