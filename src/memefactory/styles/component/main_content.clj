(ns memefactory.styles.component.main-content
  (:require [garden.def :refer [defstyles]]
            [garden.units :refer [em px]]
            [memefactory.styles.base.colors :refer [color]]
            [memefactory.styles.base.media :refer [for-media-max]]))

(defstyles core
  [:.main-content
   {:min-height (em 93)
    :padding-bottom (em 3)
    :box-shadow "inset 20px 20px 30px rgba(0,0,0,0.05)"
    :background (color :main-content-bg)}
   [:.main-content-inner
    {:max-width (px 985)
     :margin-left :auto
     :margin-right :auto
     :padding-top (em 4)}
    (for-media-max :large
                   [:&
                    {:margin-left (em 1)
                     :margin-right (em 1)}])
    (for-media-max :tablet
                   [:&
                    {:padding-top (em 1)}])]])
