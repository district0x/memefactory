(ns memefactory.styles.pages.leaderboard.creators
  (:require [garden.def :refer [defstyles]]
            [garden.units :refer [em px]]
            [memefactory.styles.base.colors :refer [color]]
            [memefactory.styles.base.fonts :refer [font]]
            [memefactory.styles.base.media :refer [for-media-max]]
            [memefactory.styles.component.panels :refer [panel-with-icon]]))

(defstyles core
  [:.leaderboard-creators-page
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
   [:section.creators
    [:.creators-panel
     (panel-with-icon {:url "/assets/icons/trophy2.svg"
                       :color :leaderboard-curator-bg})]
    [:h2.title {:white-space :normal}]
    [:.scroll-area
     {:width "100%"}
     [:.creators
      [">*"
        {:display :flex
         :min-height (em 33)
         :margin-right (em 2)
         :margin-left (em 2)
         :justify-content :left
         :flex-wrap :wrap}
        (for-media-max :large [:& {:justify-content :center}])
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
          :background-color (color :curator-card-bg)}
         [:a {:color (color :menu-text)}]
         (for-media-max :tablet [:& {:margin-left 0 :margin-right 0}])
         [:ul {:line-height (em 2)}
          [:.best-sale {:cursor :pointer
                        :font-weight :normal}]]
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
           :color (color :deep-purple)
           :cursor :pointer
           :font-size (px 13)
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
         [:&.account-tile {:background-color (color :light-green)}]]]]]
    [:div.order
     {:position :absolute
      :display :block
      :width (em 16)
      :right (em 2)
      :top (em 2)}
     [:.input-group
      [:.help-block {:display :none}]]
     (for-media-max :large
                    [:&
                     {:display :block
                      :position :unset
                      :width "92%"
                      :margin-left :auto
                      :margin-right :auto}])]]])
