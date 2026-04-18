(ns cyberleague.cli.core
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [cli-matic.core :as cli]
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

(defn list-envs! [_]
  (let [languages (r/tada! [:api/languages {}])]
    (doseq [language (->> languages
                           (sort-by :language/slug))]
      (println (str (:language/slug language) ":"))
      (doseq [env (:language/envs language)]
        (println "  " (:env/slug env))))))

(defn list-games! [_]
  (let [games (r/tada! [:api/games {}])]
    (doseq [game games]
      (println (:game/slug game)))))

(def cli-configuration
  {:command "cyberleague"
   :description "cyberleague cli for developing bots"
   :version "2026-04-18"
   :subcommands
   [{:command "login"
     :runs auth!}
    {:command "envs"
     :description "List available languages and environments"
     :runs list-envs!}
    {:command "games"
     :description "List available games"
     :runs list-games!}
    #_{:command "bots"}
    {:command "bot"
     :subcommands [{:command "new"
                    :opts [{:option "game"
                            :type :string
                            :default :present
                            :as "Game; run `cyberleague game` to see available games"}
                           {:option "env"
                            :type :string
                            :default :present
                            :as "Env; run `cyberleague envs` to see available envs"}]
                    :runs bot/new-bot!}
                   #_{:command "deploy"}
                   #_{:command "fetch"}
                   #_{:command "test"}
                   #_{:command "watch"
                      :runs (watch! (:watch options))}]}]})

(defn -main [& args]
  (cli/run-cmd args cli-configuration))

(comment

  (-main "bot" "watch" "./cli-demo/")

  (beholder/stop @watcher)

  )
