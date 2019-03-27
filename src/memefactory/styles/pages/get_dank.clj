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
   [:.spinner-outer {:margin-left :auto
                      :margin-right :auto
                      :margin-top (em -1)}]
   (for-media-max :tablet
                   [:&
                    {:margin-right (em 2)
                     :margin-left (em 2)}])
   [:.get-dank-box
    (panel-with-icon {:url "/assets/icons/dank-logo.svg"
                      :color :purple})
    [:h3.title {:white-space :normal}]
    [:.icon {:background-size [(em 4) (em 4)]
             :background-position-x (em 0.2)}]

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
