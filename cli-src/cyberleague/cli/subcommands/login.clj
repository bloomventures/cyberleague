(ns cyberleague.cli.subcommands.login
  (:require
   [clojure.string :as string]
   [cyberleague.cli.util.remote :as remote]
   [cyberleague.cli.util.token :as token]))

(defn exec! [_]
  (println "Enter token: ")
  (flush)
  (let [token (string/trim (read-line))]
    (if (re-matches token/re token)
      (do (token/save! token)
          (let [me (remote/tada! [:api/me])]
            (println (str "Welcome, " (:user/name me) "!")))
          (flush))
      (do (println "Token doesn't match format.")
          (flush)))))
