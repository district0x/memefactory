(ns memefactory.styles.component.share-buttons
  (:require [garden.def :refer [defstyles]]
            [garden.units :refer [px, em]]
            [memefactory.styles.base.media :refer [for-media-max]]))


(defstyles core
  [:.meme-detail
   [:.share-buttons
    {:position "absolute"
     :top      (em 2)
     :right    (em 2)}

    (for-media-max :tablet
                   [:&
                    {:top "auto" :right "auto" :bottom (em 2), :left (em 2)}])]
   [:.share-buttons-bottom-container
    (for-media-max :tablet
                   [:&
                    {:display "block" :height (px 32) :margin-top (em 2)}])]])