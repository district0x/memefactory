(ns memefactory.styles.component.compact-tile
  (:require
   [garden.def :refer [defstyles]]
   [garden.stylesheet :refer [at-media]]
   [clojure.string :as s]
   [memefactory.styles.base.colors :refer [color]]
   [memefactory.styles.base.media :refer [for-media-min for-media-max]]
   [garden.units :refer [px]]))

(def bar-height (px 50))

(defstyles core
  [:.compact-tile
   {:height (px 220)
    :width (px 150)
    :margin (px 10)
    :background-color (color "gray")
    :box-shadow "0 0 50px 20px rgba(0, 0, 0, 0.04)"
    :display :block}])
