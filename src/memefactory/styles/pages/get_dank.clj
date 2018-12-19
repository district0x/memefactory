(ns memefactory.styles.pages.get-dank
  (:require [garden.def :refer [defstyles]]
            [memefactory.styles.base.media :refer [for-media-min for-media-max]]
            [garden.units :refer [pt px em rem]]
            [memefactory.styles.base.colors :as c]
            [memefactory.styles.base.fonts :refer [font]]
            [memefactory.styles.component.buttons :refer [button get-dank-button]]
            [memefactory.styles.component.panels :refer [panel-with-icon]]))

(defstyles core
  [:.get-dank-page
   (for-media-max :tablet
                   [:&
                    {:margin-right (em 2)
                     :margin-left (em 2)}])
   [:.get-dank-box
    (panel-with-icon {:url "/assets/icons/get-dank-icon-bg.svg"
                      :color :redish})
    [:h3.title {:white-space :normal}]
    [:.icon {:background-size [(em 3) (em 3)]}]

    [:.form {:font-size (px 14)
             :margin (em 2)
             :height (em 4)
             :display :flex
             :justify-content :space-around
             :padding-left (em 20)
             :padding-right (em 20)}
     [:.country-code {:width (em 6.1)}]
     (for-media-max :tablet
                   [:&
                    {:padding (px 0)
                     :display :block}])]

    [:.footer
      (get-dank-button)]]])
