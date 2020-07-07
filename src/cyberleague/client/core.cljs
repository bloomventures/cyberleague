(ns ^:figwheel-hooks
  cyberleague.client.core
  (:require
    [reagent.dom :as rdom]
    [cyberleague.client.state :as state]
    [cyberleague.client.ui.app :refer [app-view]]))

(enable-console-print!)

(defn render
  []
  (rdom/render
    [app-view]
    (js/document.getElementById "app")))

(defn ^:export init []
  (render)
  (state/fetch-user!))

(defn ^:after-load reload []
  (render))
