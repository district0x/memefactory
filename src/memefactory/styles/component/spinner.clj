(ns memefactory.styles.component.spinner
  (:require
   [garden.def :refer [defstyles]]
   [garden.stylesheet :refer [at-media at-keyframes]]
   [clojure.string :as s]
   [memefactory.styles.base.colors :refer [color]]
   [memefactory.styles.base.media :refer [for-media-min for-media-max]]
   [memefactory.styles.base.fonts :refer [font]]
   [garden.units :refer [px em pt]]))

(defstyles core
  [:.spinner-outer {:width (px 174)
                    :height (px 174)
                    :z-index 5}
   [:img {:width (px 75)
          :height :auto
          :position :relative
          :z-index 10
          :top (px 64)
          :left (px 51)}]
   [:.spinner-inner
    {:animation "rotate 1.4s linear infinite"
     :width (px 174)
     :height (px 174)
     :top (px -60)
     :position :relative}]]
  (at-keyframes :rotate [:to {:transform "rotate(360deg)"}]))
