(ns memefactory.styles.component.buttons
  (:require [garden.def :refer [defstyles]]
            [garden.stylesheet :refer [at-media]]
            [clojure.string :as s]
            [memefactory.styles.base.icons :refer [icons]]
            [memefactory.styles.base.borders :refer [border-top]]
            [memefactory.styles.base.colors :as c]
            [memefactory.styles.base.fonts :refer [font]]
            [memefactory.styles.base.media :refer [for-media-min for-media-max]]
            [garden.selectors :as sel]
            [garden.units :refer [pt px em rem]]
            [clojure.string :as str]))

(defn button [{:keys [color]}]
  [:&
   (font :bungee)
   {:border-radius "1em"
    :display "block"
    :bottom (em 2)
    :height (em 2)
    :width (em 8)
    :border-style "none"
    :color (c/color :white)
    :background-color (c/color color)}
   [:&:after {:content ;;"&#8594;"
              "' →'"}]])
