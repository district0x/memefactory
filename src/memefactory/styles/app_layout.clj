(ns memefactory.styles.app-layout
  (:require [garden.def :refer [defstyles]]
            [memefactory.styles.base.media :refer [for-media-max]]
            [memefactory.styles.base.grid :refer [grid-columns]]
            [clojure.string :as s]
            [garden.units :refer [px em]]))


(defstyles core
  [:.app-container
   {:display :grid
    :min-height (em 90)}
   (grid-columns "20%" "80%")
   (for-media-max :tablet
                  [:&
                   (grid-columns "100%")])])
