(ns memefactory.styles.component.selling-panel
  (:require [garden.def :refer [defstyles]]
            [garden.units :refer [em px]]
            [memefactory.styles.base.colors :refer [color]]
            [memefactory.styles.base.media :refer [for-media-max]]
            [memefactory.styles.component.compact-tile
             :refer
             [overlay-background-footer]]))

(defstyles core
  [:.selling-panel
   [:.radio-group
    {:padding-left (em 2)
     :margin-bottom (em 1)}]
   [:.radio {:display :inline-block
             :margin-left (px 10)}
    (for-media-max :tablet
                   [:& {:display :block
                        :margin-left (px 20)}])
    [:label {:margin-left (px 8)
             :font-size (px 12)}]]
   [:.meme-card
    [:.overlay {:background overlay-background-footer}]
    [:.selling-tile-back {:height "100%"
                          :background-color (color :violet)}]]])
