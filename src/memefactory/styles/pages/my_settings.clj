(ns memefactory.styles.pages.my-settings
  (:require [garden.def :refer [defstyles]]
            [memefactory.styles.base.media :refer [for-media-min for-media-max]]
            [garden.units :refer [pt px em rem]]
            [memefactory.styles.base.colors :as c]
            [memefactory.styles.base.fonts :refer [font]]
            [memefactory.styles.component.buttons :refer [button get-dank-button]]
            [memefactory.styles.component.panels :refer [panel-with-icon]]))

(defstyles core
  [:.my-settings-page
    {:padding-top (em 2)
     :margin-right (em 6)
     :margin-left (em 6)}
    (for-media-max :tablet
                   [:&
                    {:margin-right (em 2)
                     :margin-left (em 2)}])
   [:.my-settings-box
     (panel-with-icon {:url "/assets/icons/mysettings.svg"
                       :color :sky-blue})
    [:.icon {:background-size [(em 2) (em 2)]}]
    [:.form {:font-size (px 17)
             :margin (em 2)}]
    [:.footer
      (get-dank-button)]]])
