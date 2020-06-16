(ns memefactory.styles.animations
  (:require [garden.def :refer [defstyles]]
            [garden.stylesheet :refer [at-keyframes]]))

(defstyles core
  (at-keyframes :spin
                [:from
                 {:transform "rotate(0deg)"}]
                [:to
                 {:transform "rotate(360deg)"}]))
