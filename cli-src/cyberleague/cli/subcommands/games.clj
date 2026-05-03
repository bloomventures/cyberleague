(ns cyberleague.cli.subcommands.games
  (:require
   [cyberleague.cli.util.format :as f]
   [cyberleague.cli.util.remote :as r]))

(defn exec!
  [_]
  (when-let [games (r/tada! [:api/games {}])]
    (println (f/color :color/yellow "Games:"))
    (doseq [game games]
      (println " " (:game/slug game)))))
