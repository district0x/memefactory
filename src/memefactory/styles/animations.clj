(ns memefactory.styles.animations
  (:require
   [garden.core :refer [css]]
   [garden.def :refer [defstyles]]
   [garden.stylesheet :refer [at-media at-keyframes]]
   [garden.units :refer [rem px em]]))

(defstyles core
  (at-keyframes :spin
                [:from
                 {:transform "rotate(0deg)"}]
                [:to
                 {:transform "rotate(360deg)"}]))
