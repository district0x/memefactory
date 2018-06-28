(ns memefactory.styles.component.compact-tile
  (:require
   [garden.def :refer [defstyles]]
   [garden.stylesheet :refer [at-media]]
   [clojure.string :as s]
   [memefactory.styles.base.colors :refer [color]]
   [memefactory.styles.base.media :refer [for-media-min for-media-max]]
   [garden.units :refer [px em]]))

(def bar-height (px 50))
(def card-aspect-ratio 1.5)
(def card-width 16)
(def card-height (int (* card-aspect-ratio card-width)))

(defstyles core
  [:.container
   [:.meme-card
    [:&.front :&.back
     {:backface-visibility :hidden}]
    [:&.back
     {:z-index 0}]]]
  [:.container.flipped
   [:.meme-card
    [:&.back
     {:transform "rotateY(180deg);"
      :z-index 2}]
    [:&.front
     {:transform "rotateY(0deg);"}]]]
  
  [:.meme-card
   {:position :absolute
    :background-color (color "gray")
    :border-radius "1em"
    :transition "0.3s"
    :transform-style "preserve-3d"}
   [:img
    {:width (em card-width)
     :height (em card-height)}]
   [:.info {:position :absolute
            :background-color (color :meme-bg)
            :color (color :meme-info-text)
            :top (px 0)
            :border-radius "1em"
            :bottom (px 0)
            :left (px 0)}]]
  [:.compact-tile
   {:background (color :meme-panel-bg)
    :width (em card-width)
    :height (em card-height)
    :margin (em 1)
    ;; :box-shadow "0 0 50px 20px rgba(0, 0, 0, 0.04)"
    :display :block
    :position :relative
    :perspective "1000px"
    :transform-style "preserve-3d"
    }])
