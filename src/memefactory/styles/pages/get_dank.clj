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
     (panel-with-icon {:url "/assets/icons/mysettings.svg"
                       :color :redish})
    [:.icon {:background-size [(em 2) (em 2)]}]
    [:.form {:font-size (px 14)
             :margin (em 2)
             :height (em 5)
             :display :flex
             :justify-content :space-around
             :padding-left (em 15)
             :padding-right (em 15)
             }]
    [:.footer
      (get-dank-button)]]])
