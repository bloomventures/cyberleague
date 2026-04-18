(ns cyberleague.cli.subcommands.login
  (:require
   [clojure.string :as string]
   [cyberleague.cli.util.token :as token]))

(defn exec! [_]
  (println "Enter token: ")
  (flush)
  (let [token (string/trim (read-line))]
    (if (re-matches token/re token)
      (do (spit "cli.edn" (pr-str {:token token}))
          (token/save! token)
          (println "Token saved!")
          (flush))
      (do (println "Token doesn't match format.")
          (flush)))))
