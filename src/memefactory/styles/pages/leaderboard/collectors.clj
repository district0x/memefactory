(ns memefactory.styles.pages.leaderboard.collectors
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
  [:.leaderboard-collectors-page
   [:.spinner-container {:width (px 900)
                         :height (px 500)}
    [:.spinner-outer {:margin-left :auto
                      :margin-right :auto
                      :padding-top (em 12)}]]
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
   [:section.collectors
    [:.collectors-panel
     (panel-with-icon {:url "/assets/icons/trophy2.svg"
                       :color :leaderboard-curator-bg})

     [:h2.title {:white-space :normal}]]
    [:.scroll-area
     {:width "100%"}
     [:.collectors
      [">*"
       {:display :flex
        :min-height (em 33)
        :margin-right (em 2)
        :margin-left (em 2)
        :justify-content :left
        :flex-wrap :wrap}
       (for-media-max :large [:& {:justify-content :center}])
       [:.user-tile
        {:min-width (em 18)}
        [:&.account-tile {:background-color (color :light-green)}]
        [:ul {:line-height (em 2)}
         [:a {:color (color :menu-text)}]]
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
          :cursor :pointer
          :font-size (px 13)
          :margin-top (em 2.5)
          :margin-bottom (em 2.5)
          :color (color :deep-purple)
          :overflow "hidden"
          :text-overflow "ellipsis"
          :width "100%"}]
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

        {:width (em 20)
         :display :block
         :height (em 30)
         :margin (em 1)
         :border-radius "1em"
         :padding (em 1)
         :vertical-align :middle
         :text-align :center
         :background-color (color :curator-card-bg)}]]]
     (for-media-max :tablet [:& {:margin-left 0 :margin-right 0}])

     ]

    [:div.order
     {:position :absolute
      :display :block
      :width (em 12)
      :right (em 2)
      :top (em 2)}
     [:.input-group
      [:.help-block {:display :none}]]
     (for-media-max :large
                    [:&
                     {:display :inline-block
                      :position :relative
                      :height (em 1)
                      :padding-top (em 1)
                      :width "calc(100% - 2em)"
                      :margin-left (em 3)
                      :margin-right (em 3)
                      :top (em -1)}])]]])
