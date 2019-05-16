(ns memefactory.styles.container.tiles
  (:require
   [garden.def :refer [defstyles]]
   [garden.stylesheet :refer [at-media]]
   [clojure.string :as s]
   [memefactory.styles.base.colors :refer [color]]
   [memefactory.styles.base.media :refer [for-media-min for-media-max]]
   [garden.units :refer [px em]]))

(defstyles core
  [:.tiles
   [">*"
    {:display :flex
     :overflow :visible
     :flex-wrap :wrap
     :justify-content :left
     :padding-left (em 2.5)
     :padding-right (em 2.5)}
    (for-media-max :large [:& {:justify-content :center}])
    [:.compact-tile
     {:margin-right (em 1)}
     (for-media-max :tablet [:& {:margin-right 0}])]]])
