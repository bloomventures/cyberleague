(ns cyberleague.client.ui.common)

(defn button [opts & children]
  (into [:button
         (merge {:tw "border border-white px-2 py-1 hover:bg-white/25"}
                opts)]
        children))
