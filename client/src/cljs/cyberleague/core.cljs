(ns cyberleague.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <! sub pub]]
            [clojure.string :as string]))

(enable-console-print!)

(def app-state (atom {}))


(defn games-card-view [data owner])

(defn game-card-view [data owner])

(defn rules-card-view [data owner])

(defn code-card-view [data owner])

(defn match-card-view [data owner])

(defn app-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil "Hello World!"))))

(om/root app-view app-state {:target (. js/document (getElementById "app"))})

(defn init [])
