(ns cyberleague.client.ui.styles
  (:require
    [garden.core :as garden]))

(def styles
  [:body
   {:background "red !important"}])

(def css
  (garden/css {} styles))

