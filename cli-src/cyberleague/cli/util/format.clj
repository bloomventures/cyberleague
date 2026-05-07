(ns cyberleague.cli.util.format)

(defn color
  [color s]
  (str "\u001b"
       (case color
         :color/yellow "[33m"
         :color/green  "[32m"
         :color/red    "[31m")
       s "\u001b[0m"))
