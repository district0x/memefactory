(ns memefactory.styles.component.compact-tile
  (:require
   [garden.def :refer [defstyles]]
   [garden.stylesheet :refer [at-media]]
   [clojure.string :as s]
   [memefactory.styles.base.colors :refer [color]]
   [memefactory.styles.base.media :refer [for-media-min for-media-max]]
   [memefactory.styles.base.fonts :refer [font]]
   [garden.units :refer [px em pt]]))

(def bar-height (px 50))
(def card-aspect-ratio 1.5)
(def card-width 16)
(def card-height (int (* card-aspect-ratio card-width)))

(defstyles core
  [:.container
   {:transform-style :preserve-3d
    :width (em card-width)
    :height (em card-height)
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
      :transform "rotateY(0deg)"}]]]
  [:.meme-card
   {:position :absolute
    :background-color (color "gray")
    :width (em card-width)
    :height (em card-height)
    :border-radius "1em"}
   [:img
    {:width (em card-width)
     :height (em card-height)}]
   [:.overlay {:position :absolute
               :background-color (color :meme-bg)
               :border-radius "1em"
               :top (px 0)
               :bottom (px 0)
               :left (px 0)
               :right (px 0)
               }
    [:.info {:transform "translateZ(60px) scale(0.94)"
             :color (color :meme-info-text)
             :top (px 0)
             :bottom (px 0)
             :left (px 0)
             :right (px 0)
             :position :absolute
             :perspective "inherit"}
     [:.meme-data {:text-align :center
                   :padding-left 0
                   :font-size (pt 10)
                   :list-style :none}
      [:>li {:margin-top (em 1)
             :margin-bottom (em 1)}]
      [:label {:font-weight :bold
               }]]]]]
  [:.compact-tile
   {:background (color :meme-panel-bg)
    :margin (em 1)
    ;; :box-shadow "0 0 50px 20px rgba(0, 0, 0, 0.04)"
    :display :block
    :position :relative}
   [:.footer
    {:position :relative
     :bottom 0}
    [:.title :.number-minted :.price
     {:text-align :center
      :color (color :meme-tile-footer)}]
    [:.title {:margin-top (em 1)
              :font-weight :bold}]
    [:.number-minted {:margin-top (em 0.3)}]
    [:.price
     (font :bungee)
     {:margin-top (em 0.3)}]]])
