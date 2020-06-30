(ns memefactory.styles.component.spinner
  (:require [garden.def :refer [defstyles]]
            [garden.stylesheet :refer [at-keyframes]]
            [garden.units :refer [px]]))

(defstyles core
  [:.spinner-outer {:width (px 104)
                    :height (px 104)
                    :z-index 5}
   [:img {:width (px 45)
          :height :auto
          :position :relative
          :z-index 10
          :top (px 39)
          :left (px 30)}]
   [:.spinner-path
    {:stroke-dasharray 170
     :stroke-dashoffset 20}]
   [:.spinner-inner
    {:animation "rotate 1.4s linear infinite"
     :width (px 104)
     :height (px 104)
     :top (px -36)
     :position :relative}]]
  (at-keyframes :rotate [:to {:transform "rotate(360deg)"}]))
