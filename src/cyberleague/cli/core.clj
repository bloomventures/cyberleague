(ns cyberleague.cli.core
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.cli :as cli]
   [hyperfiddle.rcf :as rcf]
   [nextjournal.beholder :as beholder]
   [cyberleague.cli.token :as token]
   [cyberleague.cli.bot :as bot]
   [cyberleague.cli.remote :as r]))

(defonce watcher (atom nil))

(defn extract-bot-name [file-name]
  (some-> (re-find #"[A-Z]{3}[-_][0-9]{4}" file-name)
          (str/replace "_" "-")))

(rcf/tests
  (extract-bot-name "ABC_1234.clj") := "ABC-1234")

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

(defn get-bot-id [bot-name]
  (-> (r/tada! [:api/bot-id-by-name
                {:bot-name bot-name}])
      :bot-id))

(defn push! [path]
  (if-let [bot-name (extract-bot-name (str (.getFileName path)))]
    (if-let [bot-id (get-bot-id bot-name)]
      (do (println "Pushing " bot-name " code to the arena!")
          (r/tada! [:api/set-bot-code!
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
  (let [languages (r/tada! [:api/languages {}])]
    (doseq [language (->> languages
                           (sort-by :language/slug))]
      (println (str (:language/slug language) ":"))
      (doseq [env (:language/envs language)]
        (println "\t" (:env/slug env))))))

(defn list-games! []
  (let [games (r/tada! [:api/games {}])]
    (doseq [game games]
      (println (:game/slug game)))))

(def cli-options
  [[nil "--auth" "authenticate"]
   [nil "--list-envs" "list available environments"]
   [nil "--list-games" "list available games"]
   [nil "--new" "create a new bot (requires --game and --env)"]
   [nil "--game GAME"]
   [nil "--env ENV"]
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
                              (bot/new-bot! (:game options) (:env options))
                              (println "Error: --new requires --game and --env"))
      (:watch options)      (watch! (:watch options))
      (:auth options)       (auth!)
      :else                 (println summary))))

(comment

  (-main "--watch" "./cli-demo/")

  (beholder/stop @watcher)

  )
