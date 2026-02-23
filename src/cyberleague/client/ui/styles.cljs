(ns cyberleague.client.ui.styles
  (:require
   [garden.core :as garden]
   [cyberleague.client.ui.colors :as colors])
  (:require-macros
   [garden.def :refer [defkeyframes]]))

(defkeyframes flash
  ["0%" {:background "default"}]
  ["45%" {:background colors/blue-dark}]
  ["55%" {:background colors/blue-dark}]
  ["100%" {:background "default"}])

(def styles
  [:body

   [:a
    {:cursor "pointer"
     :color colors/blue
     :text-decoration "none"}

    [:&:hover
     {:color colors/blue-dark}]]

   [:.graph
    [:svg
     {:margin "0 -1em"}

     [:.axis
      [:path
       :line
       {:fill "none"
        :stroke "#ccc"
        :shape-rendering "crispEdges"}]]

     [:text
      {:fill "#ccc"
       :font-size "10px"}]

     [:.line
      {:stroke colors/blue
       :fill "none"}]

     [:.area
      {:fill "#eee"}]]]

   [:.CodeMirror
    {:font-family "Inconsolata"
     :line-height 1.2
     :height "100%"
     :overflow "hidden"}

    [:.CodeMirror-lines
     {:padding-top "1em"
      :padding-left "1em"}]]])

(def css
  (garden/css flash styles))
