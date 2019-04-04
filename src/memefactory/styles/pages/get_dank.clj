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
                     :margin-top (em 4)}]
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

    [:.body
     {:min-height (em 13)}
     [:.form 
      {:display :flex
       :flex-direction :row
       :font-size (px 14)
       :padding-top (em 4)
       :height (em 4)
       :justify-content :center}
      [:.labeled-input-group
       {:padding-left (em 1)
        :padding-right (em 1)}]
      [:.country-code {:width (em 6.1)}]
      (for-media-max :tablet
                     [:&
                      {:flex-direction :column}])]]
    
    

    [:.footer
     (get-dank-button)
     {:width "100%"
      :border :none}
     [:&:disabled
      {:opacity 0.3}]]]])
