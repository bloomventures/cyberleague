(ns cyberleague.client.ui.styles
  (:require
    [garden.core :as garden]))

(def blue "#3f51b5")
(def blue-dark "#1a237e")

(def styles
  [:body
  {:background "#eeeeee"
   :margin 0 
   :padding 0
   :font-family "Inconsolata" 
   :font-size 14}
  [:&:before {
    :content ""
    :display "block"
    :position "absolute"
    :top 0
    :left 0
    :height 180
    :width "100%"
    :background "#e0e0e0"}]
  
  [:a
   {:cursor "pointer"
    :text-decoration "none"}
    [:&:hover {:color blue-dark}]]
  
  [:#app
   {:position "absolute"
    :top 0
    :right 0
    :bottom 0
    :left 0
    :overflow-y "scroll"}
   [:.app 
    {:width "100%"
     :height ":100%"}
    [:.cards
     {:position "relative"
      :height "100%"
      :white-space "nowrap"
      :padding "60px 10px 20px 10px"
      :box-sizing "border-box"}]]]

  [:.app>header
   {:position "fixed"
    :top 0
    :height "2em"
    :width "100%"
    :box-sizing "border-box"
    :padding 20
    :z-index "1000"}
   [:h1 :h2
    {:display "inline-block"
     :vertical-align "middle"
     :height "2em"
     :line-height "2em"}]
   [:h1
    {:font-weight "bold"
     :margin-right "1em"}]
   [:nav
    {:position "absolute"
     :right 20
     :top 20
     overflow "hidden"
     :height "2em"}
   [:a
    {:margin-right "1em"}]
   [:.log-out
    {:text-decoration "none"}]]
   [:.log-in
    {:display "inline-block"
     :padding "0.5em 0.75em"
     :border-radius 5
     :background blue
     :color "rgba(255,255,255,0.65)"
     :text-decoration "none"
     :vertical-align "middle"}
    [:&:hover
     {:color "white"}]
    [:&:before
     {:content ""
      :font-family "fontawesome"
      :margin-right "0.5em"}]]
   [:.user
    {:display "inline-block"
     :position "relative"
     :background blue
     :color "rgba(255,255,255,0.65)"
     :border-radius 5
     :padding-right "0.6em"
     :line-height "2em"
     :text-decoration "none"}
    [:&:hover
     {:color "white"}]
    [:img
     {:margin-top -1
      :margin-right "0.5em"
      :vertical-align "middle"
      :height "2em"
      :width "2em"
      :border-radius 5}]]]

    (at-keyframes :flash
     ["0%" {:background "default"}]
     ["45%" {:background blue-dark}]
     ["55%" {:background blue-dark}]
     ["100%" {:background "default"}])

  [:.card]
     ])

(def css
  (garden/css {} styles))
