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
   [:section.curators
    (for-media-max :tablet
                   [:& {
                        :margin-right (em 1)
                        :margin-left (em 1)}])
    [:.curators-panel
     (panel-with-icon {:url "/assets/icons/trophy2.svg"
                       :color :leaderboard-curator-bg})
     [:.icon {:margin-left (em 3)}]]
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
                      :top (em -1)}])]
    [:.scroll-area
     {:width "100%"}
     [:.curators
      {:display :flex
       :margin-right (em 2)
       :margin-left (em 2)
       :justify-content :center
       :flex-wrap :wrap
       ;; :align-content :flex-start
       }
      [:.curator
       {:min-width (em 18)}
       ["> *"
        {:display :block
         :margin-bottom (em 0.5)
         :margin-top 0}
        [:&.number
         {:color (color :purple)
          :font-size (em 3)}
         (font :bungee)]
        ]
       [:h3.address
        {:font-weight :bold
         :overflow "hidden"
         :text-overflow "ellipsis"
         :font-size (em 0.8)
         :color (color :deep-purple)
         :width "100%"}]
       [:h4
        (font :bungee)
        {:color (color :purple)
         :margin-top (em 2)
         :font-size (em 1)}]
       [:p
        {:font-weight :bold}
        [:span
         {:font-weight :normal}
         ]]

       {:width (em 20)
        :display :block
        :height (em 30)
        :margin (em 1)
        :border-radius "1em"
        :padding (em 1)
        :vertical-align :middle
        :text-align :center
        :background-color (color :curator-card-bg)}
       ]]]]])
