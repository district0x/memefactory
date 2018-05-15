(ns memefactory.styles.base.borders
  (:require [memefactory.styles.base.colors :refer [color]]
            [garden.units :refer [px]]))

(defn pseudo-border-before [params]
  [:&:before
   (merge
    {:position :absolute
     :content "''"
     :top 0
     :left 0
     :width "100%"}
    params)])

(defn border-top [params]
  {:border-top-width (get params :width (px 1))
   :border-top-color (get params :color (color :black))
   :border-top-style (get params :style :solid)})
