(ns cyberleague.cli.core
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.cli :as cli]
   [cognitect.transit :as transit]
   [hyperfiddle.rcf :as rcf]
   [nextjournal.beholder :as beholder]
   [org.httpkit.client :as http]
   [taoensso.telemere :as tel]
   [cyberleague.cli.token :as token])
  (:import
   (java.io ByteArrayOutputStream)))

(defonce watcher (atom nil))

(def api-root "http://127.0.0.1:3000")

(defn extract-bot-name [file-name]
  (some-> (re-find #"[A-Z]{3}[-_][0-9]{4}" file-name)
          (str/replace "_" "-")))

(rcf/tests
  (extract-bot-name "ABC_1234.clj") := "ABC-1234")

(defn http-request [{:keys [path method body] :as args}]
  (tel/event! ::http-request {:level :debug
                              :data args})
  @(http/request
     {:method method
      :oauth-token (token/read)
      :url (str api-root path)
      :headers {"Accept" "application/transit+json"
                "Content-Type" "application/transit+json"}
      :body (let [out (ByteArrayOutputStream. 4096)
                  writer (transit/writer out :json)]
              (transit/write writer body)
              (.toString out))
      :as :stream}
    (fn [{:keys [status body] :as response}]
      (tel/event! ::http-response {:level :debug
                                   :data response})
      (if (<= 200 status 299)
        (when (pos? (.getCount body))
          (let [b (transit/read (transit/reader body :json))]
            (tel/event! ::http-body {:level :debug
                                     :data b})
            b))
        (println "ERROR: " (slurp body))))))

(defn auth! []
  (println "Enter token: ")
  (flush)
  (let [token (str/trim (read-line))]
    (if (re-matches token/re token)
      (do (spit "cli.edn" (pr-str {:token token}))
          (token/save! token)
          (println "Token saved!")
          (flush))
      (do (println "Token doesn't match format.")
          (flush)))))

(defn tada!
  [[event-id params]]
  (http-request {:path (str "/api/tada/"
                            (namespace event-id) "." (name event-id))
                 :method :post
                 :body {:tada.event/id event-id
                        :tada.event/params params}}))

(defn get-bot-id [bot-name]
  (-> (tada! [:api/bot-id-by-name
              {:bot-name bot-name}])
      :bot-id))

(defn push! [path]
  (if-let [bot-name (extract-bot-name (str (.getFileName path)))]
    (if-let [bot-id (get-bot-id bot-name)]
      (do (println "Pushing " bot-name " code to the arena!")
          (tada! [:api/set-bot-code!
                  {:code (slurp (str path))}]))
      (println "Warning: Bot does not exist!"))
    (println "Ignoring file: Not a Bot")))

(defn watch-fn [{:keys [type path]}]
  (when (#{:modify :create} type)
    (push! path)))

(defn watch! [file-path]
  (if (.exists (io/file file-path))
    (do (reset! watcher (beholder/watch #'watch-fn file-path))
        (println "Watching files at:" file-path "👀")
        @(promise))
    (println "File does not exist!")))

(def intro
  (str/join "\n"
            ["Cyberleague CLI Tool"
             ""
             ""]))

(defn list-envs! []
  (let [languages (tada! [:api/languages {}])]
    (doseq [language (->> languages
                           (sort-by :language/slug))]
      (println (str (:language/slug language) ":"))
      (doseq [env (:language/envs language)]
        (println "\t" (:env/slug env))))))

(defn list-games! []
  (let [games (tada! [:api/games {}])]
    (doseq [game games]
      (println (:game/slug game)))))

(defn new-bot! [game-slug env-slug]
  (let [result (tada! [:api/create-bot! {:game-slug game-slug
                                         :env-slug env-slug}])]
    (if result
      (let [dir-name (str game-slug "-" (:bot/name result))]
        (io/make-parents (str dir-name "/bot.edn"))
        (spit (str dir-name "/bot.edn")
              (pr-str {:bot/id   (:bot/id result)
                       :bot/name (:bot/name result)
                       :bot/env  env-slug
                       :bot/game game-slug}))
        (println "Created" dir-name))
      (println "Error: failed to create bot" result))))

(def cli-options
  [[nil "--auth" "authenticate"]
   [nil "--list-envs" "list available environments"]
   [nil "--list-games" "list available games"]
   [nil "--new" "create a new bot (requires --game and --env)"]
   [nil "--game SLUG" "game slug"]
   [nil "--env ENV" "bot environment (e.g. clojure-sci, javascript-v8)"]
   ["-w" "--watch FILE" "watch a bot file"]
   ["-h" "--help" "show this help"]])

(defn -main [& opts]
  (let [{:keys [options summary errors]} (cli/parse-opts opts cli-options)
        summary (str intro summary)]
    (cond
      (:help options)       (println summary)
      (:list-envs options)  (list-envs!)
      (:list-games options) (list-games!)
      (:new options)        (if (and (:game options) (:env options))
                              (new-bot! (:game options) (:env options))
                              (println "Error: --new requires --game and --env"))
      (:watch options)      (watch! (:watch options))
      (:auth options)       (auth!)
      :else                 (println summary))))

(comment

  (-main "--watch" "./cli-demo/")

  (beholder/stop @watcher)

  )
