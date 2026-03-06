(ns cyberleague.client.ui.common
  (:require
   [bloom.commons.fontawesome :as fa]))

(defn button [opts & children]
  (into [:button
         (merge {:tw "border border-white text-white px-2 py-1 hover:bg-white/25"}
                opts)]
        children))

(defn nav-button [opts & children]
  (into [:a
         (merge {:tw "bg-white/65 text-#3f51b5 font-bold px-1 py-0.5 rounded hover:bg-white"}
                opts)]
        children))

(defn nav-link [opts & children]
  (into [:a
         (merge {:tw "text-white/65 hover:text-white whitespace-nowrap"}
                opts)]
        children))

(defn bot-chip [bot]
  [:div {:tw "inline-flex items-center gap-1 justify-between"}
   [:span (if (= :active (:bot/status bot))
            "●"
            "○")]
   [:span
    (:user/name (:bot/user bot))
    "/"
    (:bot/name bot)]

   [:span {:tw "grow"}]

   [:span {:tw "whitespace-nowrap flex items-center"}
    " ("
    (:bot/weight bot)
    [fa/fa-weight-hanging-solid
     {:tw "w-0.75em h-0.75em inline ml-0.5"}]
    ")"]])
