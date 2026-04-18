(ns cyberleague.cli.subcommands.bot.new
  (:require
   [clojure.java.io :as io]
   [cyberleague.cli.util.remote :as r]
   [cyberleague.cli.util.ednf :as ednf]))

(defn create-bot!
  [{game-slug :game env-slug :env}]
  (let [result (r/tada! [:api/create-bot! {:game-slug game-slug
                                           :env-slug env-slug}])]
    (if result
      (if-let [bot (r/tada! [:api/bot-code {:bot-id (:bot/id result)}])]
        (let [dir-name (str game-slug "-" (:bot/name bot))
              path (str dir-name "/bot.edn")]
          (io/make-parents path)
          (ednf/write! path
                       {:bot/id   (:bot/id bot)
                        :bot/name (:bot/name bot)
                        :bot/env  env-slug
                        :bot/game game-slug})
          ;; TODO hardcoding clj for now, will have to rethink
          (spit (str dir-name "/bot.clj")
                (:code/code (:bot/code bot)))
          (println "Created" dir-name))
        (println "Error: bot not found"))
      (println "Error: failed to create bot"))))
