(ns cyberleague.cli.bot
  (:require
   [clojure.java.io :as io]
   [cyberleague.cli.remote :as r]
   [cyberleague.cli.ednf :as ednf]))

(defn new-bot!
  [game-slug env-slug]
  (let [result (r/tada! [:api/create-bot! {:game-slug game-slug
                                           :env-slug env-slug}])]
    (if result
      (let [dir-name (str game-slug "-" (:bot/name result))
            path (str dir-name "/bot.edn")]
        (io/make-parents path)
        (ednf/write! path
                     {:bot/id   (:bot/id result)
                      :bot/name (:bot/name result)
                      :bot/env  env-slug
                      :bot/game game-slug})
        (println "Created" dir-name))
      (println "Error: failed to create bot" result))))
