(ns cyberleague.cli.subcommands.games
  (:require
   [cyberleague.cli.util.remote :as r]))

(defn exec!
  [_]
  (let [games (r/tada! [:api/games {}])]
    (doseq [game games]
      (println (:game/slug game)))))
