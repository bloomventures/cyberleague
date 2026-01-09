(ns cyberleague.client.ui.common)

(defn button [opts & children]
  (into [:button
         (merge {:tw "border border-white text-white px-2 py-1 hover:bg-white/25"}
                opts)]
        children))

(defn bot-chip [bot]
  [:div {:tw "inline-flex items-center gap-1"}
   [:span (if (= :active (:bot/status bot))
     "●"
     "○")]
   [:span (:user/name (:bot/user bot))
   "/"
   (:bot/name bot)]])
