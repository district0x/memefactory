(ns memefactory.styles.pages.bridge
  (:require [garden.def :refer [defstyles]]
            [garden.units :refer [em px]]
            [memefactory.styles.base.colors :refer [color]]
            [memefactory.styles.base.media :refer [for-media-max]]
            [memefactory.styles.component.buttons :refer [button get-dank-button]]
            [memefactory.styles.component.compact-tile :as compact-tile]
            [memefactory.styles.component.panels :refer [panel-with-icon]]))

(defstyles core
  [:.bridge-page
   [:.bridge-box
     (panel-with-icon {:url "/assets/icons/matic.svg"
                      :color :sky-blue})
    [:.icon {:background-size [(em 2.5) (em 2)]}]
    [:p
     {:margin-left (em 1)
      :padding-top (em 2)
      :padding-left (em 3)
      :padding-right (em 3)}
     ]
    [">div"
     {:box-shadow ".3em .3em 0px 0px rgba(0,0,0,0.05)"
      :background-color (color :white)
      :border-radius "1em"
      :position :relative
      :padding (em 2)}
     [:&.form-panel
      {:margin-left (em 1)
       :padding-top (em 2)
       :padding-left (em 3)
       :padding-right (em 3)}
      [:.submit {:display :flex
                 :margin-top (em 1)
                 :justify-content :flex-start
                 :align-items :center}
       [:button
        {:padding-top (em 0.2)
         :margin-left (em 0.5)
         :margin-right (em 0.5)
         :font-size (px 12)}
        (button {:background-color :purple
                 :color :white
                 :height (em 3.3)
                 :width (em 10)})]]
      [:.not-enough-dank {:margin-top (em 1)
                          :color (color :redish)}]]]

    ]
   [:.footer
    {:color :gray}]
   ])
