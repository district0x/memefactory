(ns memefactory.styles.component.discord
  (:require [garden.def :refer [defstyles]]
            [garden.stylesheet :refer [at-keyframes]]
            [garden.units :refer [px]]
            [memefactory.styles.base.media :refer [for-media-max]]))

(defstyles core
  [:.discord-button {
                     :position "fixed"
                     :z-index 2147483000
                     :right (px 20)
                     :bottom (px 20)
                     :height (px 56)
                     :width (px 56)
                     :border-radius (px 56)
                     :background-color "rgb(88, 101, 242)"
                     :box-shadow "rgba(88, 101, 242, 0.3) 0px 3px 5px -1px, rgba(88, 101, 242, 0.14) 0px 6px 10px 0px, rgba(88, 101, 242, 0.12) 0px 1px 18px 0px"
                     :text-align "center"}
   (for-media-max :tablet
                  [:& {
                   :right (px 0)
                   :bottom (px 0)
                   :height (px 40)
                   :width (px 40)
                   :border-radius (px 40)}])
   [:img {
          :height "44%"
          :position "relative"
          :top "50%"
          :-webkit-transform "translateY(-50%)"
          :-ms-transform "translateY(-50%)"
          :transform "translateY(-50%)"}]])
