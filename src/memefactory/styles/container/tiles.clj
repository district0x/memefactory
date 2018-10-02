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
   {:display :flex
    :flex-wrap :wrap
    :margin-right (em 6)
    :margin-left (em 6)
    :justify-content :space-around}
   (for-media-max :tablet
                  [:& {
                       :margin-right (em 1)
                       :margin-left (em 1)
                       }])
   [">*"
    {:flex "1 0 calc(26% - 1em)"}]])
