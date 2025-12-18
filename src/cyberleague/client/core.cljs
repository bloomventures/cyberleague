(ns ^:figwheel-hooks
  cyberleague.client.core
  (:require
    [bloom.omni.reagent :as rdom]
    [cyberleague.games.games] ;; so games get registered
    [cyberleague.client.state :as state]
    [cyberleague.client.ui.app :refer [app-view]]))

(enable-console-print!)

(defn render
  []
  (rdom/render
    [app-view]))

(defn ^:export init []
  (render)
  (state/fetch-user!))

(defn ^:after-load reload []
  (render))
