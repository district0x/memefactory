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
    :justify-content :space-around
    :padding-left (em 2.5)
    :padding-right (em 2.5)
    }
   (for-media-max :large
                  [:& {
                       :margin-right (em 1)
                       :margin-left (em 1)
                       }])
   [">*"
    {:flex "1 0 calc(26% - 1em)"}]])
