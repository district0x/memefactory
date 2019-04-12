(ns memefactory.styles.pages.leaderboard.curators
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
  [:.leaderboard-curators-page
   [:.spinner-container {:width (px 900)
                         :height (px 500)}
    [:.spinner-outer {:margin-left :auto
                      :margin-right :auto
                      :padding-top (em 12)}]]
   [:section.curators
    [:.curators-panel
     (panel-with-icon {:url "/assets/icons/trophy2.svg"
                       :color :leaderboard-curator-bg})]
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
                     {:display :inline-block
                      :position :relative
                      :height (em 1)
                      :padding-top (em 1)
                      :width "calc(100% - 2em)"
                      :margin-left (em 3)
                      :margin-right (em 3)
                      :top (em -1)}])]
    [:.scroll-area
     {:width "100%"}
     [:.curators
      [">*"
       {:display :flex
        :min-height (em 33)
        :margin-right (em 2)
        :margin-left (em 2)
        :justify-content :left
        :flex-wrap :wrap}
       (for-media-max :large [:& {:justify-content :center}])
       [:.curator
        {:min-width (em 18)
         :padding-top (em 2)
         :padding-left (em 1)
         :padding-right (em 1)}
        (for-media-max :tablet
                       [:&
                        {:margin-left (em 0)
                         :margin-right (em 0)}])
        ["> *"
         {:display :block
          :margin-bottom (em 0.4)
          :margin-top 0}
         [:&.number
          {:color (color :purple)
           :font-size (em 3)}
          (font :bungee)]
         ]
        [:h3.address
         {:font-weight :bold
          :cursor :pointer
          :font-size (px 13)
          :overflow "hidden"
          :text-overflow "ellipsis"
          :color (color :deep-purple)
          :width "100%"}]
        [:h4
         (font :bungee)
         {:color (color :purple)
          :margin-top (em 2)
          :font-size (em 1)}]
        [:p
         {:font-weight :bold
          :color (color :section-subcaption)}
         [:&.total-earnings
          {:margin-top (em 2)}]
         [:span
          {:font-weight :normal}]]


        {:width (em 20)
         :display :block
         :height (em 30)
         :margin (em 1)
         :border-radius "1em"
         :vertical-align :middle
         :text-align :center
         :background-color (color :curator-card-bg)}
        [:&.account-tile {:background-color (color :light-green)}]
        (for-media-max :tablet [:& {:margin-left 0 :margin-right 0}])
        ]]]]]])
