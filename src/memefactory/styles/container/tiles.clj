(ns memefactory.styles.container.tiles
  (:require
   [garden.def :refer [defstyles]]
   [garden.stylesheet :refer [at-media]]
   [clojure.string :as s]
   [memefactory.styles.base.colors :refer [color]]
   [memefactory.styles.base.media :refer [for-media-min for-media-max]]
   [garden.units :refer [px]]))

(defstyles core
  [:.tiles
   {:display :flex
    :flex-wrap :wrap
    :justify-content :space-around}
   [">*"
    {:flex "1 0 calc(26% - 1em)"}]])
