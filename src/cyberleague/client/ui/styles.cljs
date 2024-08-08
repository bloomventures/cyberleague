(ns cyberleague.client.ui.styles
  (:require
    [garden.core :as garden]
    [cyberleague.game-registrar :as registrar]
    [cyberleague.client.ui.colors :as colors])
  (:require-macros
    [garden.def :refer [defkeyframes]]))

(defkeyframes flash
  ["0%" {:background "default"}]
  ["45%" {:background colors/blue-dark}]
  ["55%" {:background colors/blue-dark}]
  ["100%" {:background "default"}])

(defn >results []
  (->> @registrar/games
       vals
       (mapv (fn [game]
               ((:game.config/match-results-styles game))))))

(defn >match-results []
  [:>.match-results
   [:>.scrubber
    [:>.row
     {:display "flex"
      :justify-content "space-between"
      :align-items "center"}]
    [:>input
     {:width "100%"}]]

   (>results)])

(def styles
  [:body
   {:background "#eeeeee"
    :margin 0
    :padding 0
    :font-family "Inconsolata"
    :font-size "14px"}

   [:&:before
    {:content "\"\""
     :display "block"
     :position "absolute"
     :top 0
     :left 0
     :height "180px"
     :width "100%"
     :background "#e0e0e0"
     :z-index -1}]

   [:a
    {:cursor "pointer"
     :color colors/blue
     :text-decoration "none"}

    [:&:hover
     {:color colors/blue-dark}]]

   [:.button
    {:display "inline-block"
     :padding "0.5em 0.75em"
     :border-radius "5px"
     :background "#9fa8da"
     :color "white"
     :text-decoration "none"
     :vertical-align "middle"}

    [:&:hover
     {:background "#c5cae9"}]]

   [:#app

    [:>.app
     {:height "100%"
      :display "flex"
      :flex-direction "column"}

     [:>header
      {:padding "20px"
       :display "flex"
       :justify-content "space-between"}

      [:>h1
       :>h2
       {:display "inline-block"
        :vertical-align "middle"
        :height "2em"
        :line-height "2em"}]

      [:>h1
       {:font-weight "bold"
        :margin-right "1em"}]

      [:>h2
       {:flex-grow 1}]

      [:>nav

       [:>a
        {:margin-right "1em"}

        [:&.user
         {:display "inline-block"
          :position "relative"
          :background colors/blue
          :color "rgba(255,255,255,0.65)"
          :border-radius 5
          :padding-right "0.6em"
          :line-height "2em"
          :text-decoration "none"}

         [:&:hover
          {:color "white"}]

         [:>img
          {:margin-top -1
           :margin-right "0.5em"
           :vertical-align "middle"
           :height "2em"
           :width "2em"
           :border-radius "5px"}]]

        [:&.log-in
         {:display "inline-block"
          :padding "0.5em 0.75em"
          :border-radius "5px"
          :background colors/blue
          :color "rgba(255,255,255,0.65)"
          :text-decoration "none"
          :vertical-align "middle"}

         [:&:hover
          {:color "white"}]

         [:&:before
          {:content ""
           :font-family "fontawesome"
           :margin-right "0.5em"}]]

        [:&.log-out
         {:text-decoration "none"}]]]]

     [:>.cards
      {:padding "0 10px 20px 10px"
       :flex-grow 1
       :display "flex"
       :overflow-y "hidden"}

      [:>.card
       {:background "white"
        :margin "0 10px"
        :flex-shrink 0
        :box-shadow "0 1px 1.5px 0 rgba(0,0,0,0.12)"
        :display "flex"
        :flex-direction "column"}

       [:>header
        {:background colors/blue
         :color "white"
         :padding "1em"
         :animation [[flash "1s" "ease" "0s" "1" "normal"]]
         :display "flex"
         :justify-content "space-between"}

        [:>.gap
         {:flex-grow 2}]

        [:>span
         {:margin-right "1em"}]

        [:>a
         {:color "rgba(255,255,255,0.65)"
          :margin-right "1em"
          :text-decoration "none"}

         [:&:hover
          {:color "white"}]

         [:&.close
          {:text-decoration "none"
           :text-align "center"
           :padding "0.5em"
           :margin "-0.5em"}]

         [:&.button
          {:text-decoration "none"
           :margin "-0.25em 1em"
           :padding "0.25em 0.35em"
           :color colors/blue
           :font-weight "bold"
           :background "rgba(255,255,255,0.65)"}

          [:&:hover
           {:color colors/blue
            :background "white"}]]]]

       [:>.content
        {:padding "1em"
         :flex-grow 1
         :overflow-y "auto"
         :overflow-x "hidden"
         :box-sizing "border-box"
         :line-height 1.15}

        [:p
         {:margin-bottom "1em"}]

        [:h2
         {:font-weight "bold"
          :margin-bottom "1em"}]

        [:table
         {:margin "0 auto"}

         [:th
          {:text-align "left"
           :font-weight "bold"
           :padding "0.25em"}]

         [:td
          {:padding "0.25em"
           :text-align "left"}]]]

       [:&.game

        [:p
         {:max-width "30em"}]

        [:table

         [:>tbody

          [:>tr

           [:>td
            {:vertical-align "middle"}

            [:>.bar
             {:background colors/blue-light
              :width "1em" ;; overriden inline
              :height "0.5em"}]]]]]]

       [:&.bot

        [:>header
         [:>.bot-name
          :>.game-name
          {:white-space "nowrap"}]]

        [:>.content

         [:>.graph

          [:>svg
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
            {:fill "#eee"}]]]]]

       [:&.match

        [:>.content

         [:>h1
          {:text-align "center"
           :margin-bottom "1em"}

          [:>.winner
           [:&:before
            {:content "\" \""
             :font-family "fontawesome"}]]]

         (>match-results)]]

       [:&.code
        {:min-width "800px"}

        [:>header

         [:>.status
          {:margin-left "1em"}

          [:>.button
           {:line-height 1
            :margin "-0.5em 1em"}]]]

        [:>.content
         {:padding 0
          :display "flex"}

         [:>.lang-pick
          :>.source
          :>.test
          {:box-sizing "border-box"}]

         [:>.source
          {:width "70%"}

          [:>.wrapper
           [:>.CodeMirror
            {:font-family "Inconsolata"
             :line-height 1.2
             :height "100%"
             :overflow "hidden"}

            [:.CodeMirror-lines
             {:padding-top "1em"}]]]]

         [:>.lang-pick
          {:padding "1em"
           :line-height 1.5}

          [:>h2
           {:margin-bottom 0}]

          [:>a
           {:display "block"}]]

         [:>.test
          {:flex-grow 0
           :background "#f7f7f7"
           :border-left "1px solid #ddd"}

          (>match-results)]]]]]
     [:.card.user>.content>.token-management
      {:background "#9fa8da"
       :margin "-1em -1em 1em -1em"
       :padding "1em"
       :color "white"}]]]])

(def css
  (garden/css flash styles))
