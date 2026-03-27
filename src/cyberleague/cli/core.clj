(ns cyberleague.cli.core
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.cli :as cli]
   [cognitect.transit :as transit]
   [hyperfiddle.rcf :as rcf]
   [nextjournal.beholder :as beholder]
   [org.httpkit.client :as http])
  (:import
   (java.io ByteArrayOutputStream)))

(defonce watcher (atom nil))

(def api-root "http://127.0.0.1:3000")

(defn extract-bot-name [file-name]
  (some-> (re-find #"[A-Z]{3}[-_][0-9]{4}" file-name)
          (str/replace "_" "-")))

(rcf/tests
  (extract-bot-name "ABC_1234.clj") := "ABC-1234")

(defn get-token []
  (let [f (io/file "cli.edn")]
    (when (.exists f)
      (:token (edn/read-string (slurp f))))))

(def token-re
  #"^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$")

(defn http-request [{:keys [path method body]}]
  @(http/request
     {:method method
      :oauth-token (get-token)
      :url (str api-root path)
      :headers {"Accept" "application/transit+json"
                "Content-Type" "application/transit+json"}
      :body (let [out (ByteArrayOutputStream. 4096)
                  writer (transit/writer out :json)]
              (transit/write writer body)
              (.toString out))
      :as :stream}
     (fn [{:keys [status body]}]
       (if (<= 200 status 299)
         (when (pos? (.getCount body))
           (transit/read (transit/reader body :json)))
         (println "ERROR: " (slurp body))))))

(defn get-bot-id [bot-name]
  (-> (http-request {:path   "/api/bots/get-id"
                     :method :post
                     :body   {:bot-name bot-name}})
      :bot-id))

(defn push! [path]
  (if-let [bot-name (extract-bot-name (str (.getFileName path)))]
    (if-let [bot-id (get-bot-id bot-name)]
      (do (println "Pushing " bot-name " code to the arena!")
          (http-request {:path   (str "/api/bots/" bot-id "/code")
                         :method :put
                         :body   {:bot-code (slurp (str path))}}))
      (println "Warning: Bot does not exist!"))
    (println "Ignoring file: Not a Bot")))

(defn watch-fn [{:keys [type path]}]
  (when (#{:modify :create} type)
    (push! path)))

(defn auth! []
  (println "Enter token: ")
  (flush)
  (let [token (str/trim (read-line))]
    (if (re-matches token-re token)
      (do (spit "cli.edn" (pr-str {:token token}))
          (println "Token saved!")
          (flush))
      (do (println "Token doesn't match format.")
          (flush)))))

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
  (let [envs (http-request {:path "/api/envs"
                            :method :get
                            :body nil})]
    (doseq [env envs]
      (println env))))

(def cli-options
  [[nil "--auth" "authenticate"]
   [nil "--list-envs" "list available environments"]
   ["-w" "--watch FILE" "watch a bot file"]
   ["-h" "--help" "show this help"]])

(defn -main [& opts]
  (let [{:keys [options summary]} (cli/parse-opts opts cli-options)
        summary (str intro summary)]
    (cond
      (:help options)  (println summary)
      (:list-envs options)  (list-envs!)
      (:watch options) (watch! (:watch options))
      (:auth options)  (auth!)
      :else            (println summary))))

(comment

  (-main "--watch" "./cli-demo/")

  (beholder/stop @watcher)

  )
