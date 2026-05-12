(ns cyberleague.client.ui.common
  (:require
   [clojure.pprint :as pprint]
   [bloom.commons.fontawesome :as fa]
   [markdown.core :as markdown]
   [reagent.core :as r]))

(defn markdown
  [s]
  [:div.prose
   {:dangerouslySetInnerHTML
    (r/unsafe-html (markdown/md->html s))}])

(defn pretty-print
  [code-string]
  (with-out-str
    (pprint/pprint code-string)))

(defn button [opts & children]
  (into [:button
         (merge {:tw "border border-white text-white px-2 py-1 hover:bg-white/25"}
                opts)]
        children))

(defn body-link
  [opts & children]
  (into [:a
         (merge {:tw "bg-#3f51b5 text-white font-bold px-1 py-0.5 rounded hover:bg-#1a237e hover:text-white inline-block"}
                opts)]
        children))

(defn body-button
  [opts & children]
  (into [:button
         (merge {:tw "bg-#3f51b5 text-white font-bold px-1 py-0.5 rounded hover:bg-#1a237e hover:text-white inline-block"}
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

(defn color [x]
  (if x
    (str "oklch(50% 70% " (hash x) ")")
    "#000000"))

(defn artifact-chip
  [digest]
  (let [digest (or digest "??????")]
    [:div {:tw "rounded text-white px-1 inline-block"
           :style {:background-color (color digest)}}
     (subs digest 0 6)]))

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

   ;; only active bots have weights
   ;; disable for now - not enough space
   ;; brinb back when we make this a hover-tooltip
   #_(when-let [weight (:bot/weight bot)]
       (when (< 0 weight)
         [:span {:tw "whitespace-nowrap flex items-center"}
          " ("
          weight
          [fa/fa-weight-hanging-solid
           {:tw "w-0.75em h-0.75em inline ml-0.5"}]
          ")"]))])

(defn subheading [s]
  [:div {:tw "border-b border-solid text-#3f51b5 border-#3f51b5"}
   s])
