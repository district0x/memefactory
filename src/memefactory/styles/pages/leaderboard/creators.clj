(ns memefactory.styles.pages.leaderboard.creators
  (:require [garden.def :refer [defstyles]]
            [garden.stylesheet :refer [at-media]]
            [clojure.string :as s]
            [memefactory.styles.base.icons :refer [icons]]
            [memefactory.styles.base.borders :refer [border-top border-bottom]]
            [memefactory.styles.base.colors :refer [color]]
            [memefactory.styles.base.fonts :refer [font]]
            [memefactory.styles.base.media :refer [for-media-min for-media-max]]
            [memefactory.styles.component.search :refer [search-panel]]
            [garden.selectors :as sel]
            [garden.units :refer [pt px em rem]]
            [memefactory.styles.component.panels :refer [panel-with-icon]]
            [clojure.string :as str]))

(defstyles core
  [:.leaderboard-creators-page
   [:.loading
    {;;:flex :none
     :color (color :busy-grey)
     :border-top-color (color :white)
     :margin :auto
     :border-width (em 1)
     :border-top-width (em 1)
     :width (em 7)
     :height (em 7)
     :border-style :solid
     :border-top-style :solid
     :border-radius "50%"
     :animation-name :spin
     :animation-duration "2s"
     :animation-iteration-count :infinite
     :animation-timing-function :linear}]
   [:section.creators
    [:.creators-panel
     (panel-with-icon {:url "/assets/icons/leaderboardicon.png"
                       :color :leaderboard-curator-bg})
     [:.icon {:background-position-x (em 0)
              :margin-left (em 3)}]]
    [:h2.title {:white-space :normal}]
    [:.scroll-area
     {:width "100%"}
     [:.creators
      {:display :flex
       :margin-right (em 2)
       :margin-left (em 2)
       :justify-content :center
       :flex-wrap :wrap}
      [:.user-tile
       {:min-width (em 18)
        :width (em 20)
        :display :block
        :height (em 30)
        :margin (em 1)
        :border-radius "1em"
        :padding (em 1)
        :vertical-align :middle
        :text-align :center
        :background-color (color :curator-card-bg)
        }
       [:ul {:line-height (em 2)}]
       ["> *"
        {:display :block
         :margin-bottom (em 0.5)
         :list-style :none
         :padding-left 0
         :margin-top 0}
        [:&.number
         {:color (color :purple)
          :margin-top (em 3)
          :font-size (em 3)}
         (font :bungee)]
        ]
       [:.user-address
        {:font-weight :bold
         :overflow "hidden"
         :text-overflow "ellipsis"
         :width "100%"
         :margin-top (em 2.3)
         :margin-bottom (em 1.7)}]
       [:h4
        (font :bungee)
        {:color (color :purple)
         :margin-top (em 2)
         :font-size (em 1)}]
       [:li
        {:font-weight :bold
         :color (color :section-subcaption)}
        [:label {:margin-right (em 0.3)}]
        [:span
         {:font-weight :normal}]]
       #_{:width (em 20)
          :display :block
          :height (em 30)
          :margin (em 1)
          :border-radius "1em"
          :padding (em 1)
          :vertical-align :middle
          :text-align :center
          :background-color (color :curator-card-bg)}
       ]]]
    [:div.order
     {:position :absolute
      :display :block
      :right (em 2)
      :top (em 2)}
     [:.input-group
      [:.help-block {:display :none}]]
     (for-media-max :tablet
                    [:&
                     {:display :inline-block
                      :position :relative
                      :height (em 1)
                      :padding-top (em 1)
                      :width "calc(100% - 2em)"
                      :margin-left (em 3)
                      :margin-right (em 3)
                      :top (em -1)}])]]])
