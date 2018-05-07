(ns memefactory.styles.app-layout
  (:require [garden.def :refer [defstyles]]
            [memefactory.styles.base.media :refer [for-media-max]]
            [memefactory.styles.base.grid :refer [grid-columns]]
            [clojure.string :as s]
            [garden.units :refer [px]]))


(defstyles core
  [:.app-container
   {:display :grid}
   (grid-columns "20%" "80%")
   (for-media-max :mobile
                  [:&
                   (grid-columns "100%")])])
