(ns cyberleague.client.ui.styles
  (:require-macros
   [garden.def :refer [defkeyframes]])
  (:require
   [garden.core :as garden]
   [cyberleague.client.ui.colors :as colors]))

(defkeyframes flash
  ["0%" {:background "default"}]
  ["45%" {:background colors/blue-dark}]
  ["55%" {:background colors/blue-dark}]
  ["100%" {:background "default"}])

(def styles
  [:body

   [:.prose
    [:ul
     {:list-style-type "disc"
      :padding-inline-start "1.625em"}
     [:li
      {:margin-top "0.5em"
       :margin-bottom "0.5em"}]]]

   [:a
    {:cursor "pointer"
     :color colors/blue
     :text-decoration "none"}

    [:&:hover
     {:color colors/blue-dark}]]])

(def css
  (garden/css flash styles))
