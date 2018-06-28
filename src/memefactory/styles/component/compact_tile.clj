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
   {:transform-style :preserve-3d
    :perspective (px 1000)}
   [:.meme-card
    [:&.front :&.back
     {:transition "transform .7s cubic-bezier(0.4, 0.2, 0.2, 1)"}]
    [:&.front
     {:text-align :center
      :backface-visibility :hidden
      :transform-style :preserve-3d
      :transform "rotateY(0deg)"}]
    [:&.back
     {:transform-style :preserve-3d
      :transform "rotateY(180deg)"}]]]
  [:.container.flipped
   [:.meme-card
    [:&.front :&.back
     {:transition "transform .7s cubic-bezier(0.4, 0.2, 0.2, 1)"}]
    [:&.front
     {:transform-style :preserve-3d
      :transform "rotateY(-180deg)"}]
    [:&.back
     {:transform-style :preserve-3d
      :transform "rotateY(0deg)"}]
    ]]
  
  [:.meme-card
   {:position :absolute
    :background-color (color "gray")
    :border-radius "1em"}
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
    :position :relative}])
