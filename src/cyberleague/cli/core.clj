(ns cyberleague.cli.core
  (:require [clojure.tools.cli :as cli]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [org.httpkit.client :as http]
            [nextjournal.beholder :as beholder]
            [cognitect.transit :as transit]
            [clojure.string :as str])
  (:import [java.io ByteArrayOutputStream]))

(defonce watcher (atom nil))

(def api-root "http://127.0.0.1:3000/")

(defn extract-bot-name [file-name]
  (re-find #"[A-Z]{3}-[0-9]{4}" file-name))

(defn get-token []
  (:token (edn/read-string (slurp "cli.edn"))))

(defn http-request [{:keys [path method body]}]
  @(http/request
     {:method method
      :url (str api-root path)
      :headers {"Accept" "application/transit+json"}
      :body (let [out (ByteArrayOutputStream. 4096)
                  writer (transit/writer out :json)]
              (transit/write writer body))
      :as :stream}
     (fn [{:keys [status body]}]
       (if (= status 200)
         (transit/read (transit/reader body :json))
         nil))))

(defn push! [path]
  (if-let [bot-name (extract-bot-name (str (.getFileName path)))]
    (do (println "Pushing " bot-name " code to the arena!")
        (http-request {:path        (str "/api/bots/" bot-name "/code")
                       :method      :put
                       :oauth-token (get-token)
                       :body        {:bot/code (slurp path)}}))
    (println "Ignoring file: Not a Bot")
    ))

(defn watch-fn [{:keys [type path]}]
  (when (#{:modify :create} type)
    (push! path)))

(defn auth! [])

(defn watch! [file-path]
  (if (.exists (io/file file-path))
    (reset! watcher (beholder/watch #'watch-fn file-path))
    (println "File does not exist!")))

(defn -main [& opts]
  (let [{:keys [options]} (cli/parse-opts opts [[nil "--auth" "authenticate"]
                                                ["-w" "--watch FILE" "watch a bot file"]])]
    (cond
      (:watch options) (watch! (:watch options))
      (:auth options)  (auth!)
      )))

(comment

  (-main "--watch" "./cli-demo/")

  (beholder/stop @watcher)

  )
