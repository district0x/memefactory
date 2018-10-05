(ns memefactory.styles.component.compact-tile
  (:require
   [garden.def :refer [defstyles]]
   [garden.stylesheet :refer [at-media]]
   [clojure.string :as s]
   [memefactory.styles.base.colors :refer [color]]
   [memefactory.styles.base.media :refer [for-media-min for-media-max]]
   [memefactory.styles.base.fonts :refer [font]]
   [memefactory.styles.base.fonts :refer [font]]
   [memefactory.styles.component.buttons :refer [button]]
   [garden.units :refer [px em pt]]))

(def bar-height (px 50))
(def card-aspect-ratio 1.5)
(def card-width 16)
(def card-height (int (* card-aspect-ratio card-width)))

(defstyles core
  [:.container
   {:transform-style :preserve-3d
    :margin-right :auto
    :margin-left :auto
    :left 0
    :right 0
    :width (em card-width)
    :height (em card-height)
    :perspective (px 10000)}
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
    :border-radius "1em"
    :border "1px solid"
    :border-color (color :light-pink)
    :overflow :hidden}
   [:.meme-placehodler
    [">*"
     {:position :absolute;
      :margin :auto
      :top 0
      :left 0
      :right 0
      :bottom 0}]]
   [:.meme-image
    {:width (em card-width)
     :height (em card-height)}]
   [:.overlay {:position :absolute
               ;; :background-color (color :meme-bg)
               :background (str "linear-gradient(to bottom, " (color :meme-bg) " 0%," (color :meme-bg) " 75%," (color :meme-bg-bottom) " 75%," (color :meme-bg-bottom) " 100%)")
               :border-radius "1em"
               :top (px 0)
               :bottom (px 0)
               :left (px 0)
               :right (px 0)}
    [:.info {:transform "translateZ(60px)"
             :color (color :meme-info-text)
             :top (px 0)
             :bottom (px 0)
             :left (px 0)
             :right (px 0)
             :position :absolute
             :text-align :center
             :perspective "inherit"}
     [:hr {:margin (em 1)}]
     [:button
      {:right 0
       :left 0
       :margin-right :auto
       :margin-left :auto
       :position "absolute"}
      (button {:background-color :meme-buy-button
               :color :light-light-grey})
      [:&:after {:content ;;"&#8594;"
                 "' →'"}]]
     [:.meme-data {:text-align :center
                   :padding-left 0
                   :font-size (pt 10)
                   :list-style :none}
      [:>li {:margin-top (em 1)
             :margin-bottom (em 1)}]
      [:label {:font-weight :bold}]
      [:span {:overflow :hidden
              :text-overflow :ellipsis
              :white-space :nowrap}]]]]]
  [:.compact-tile
   {:background (color :meme-panel-bg)
    :width (em card-width)
    :margin (em 1)
    ;; :box-shadow "0 0 50px 20px rgba(0, 0, 0, 0.04)"
    :display :block
    :position :relative
    :text-align :center}
   [:.footer
    {:position :relative
     :bottom 0}
    [:.title :.number-minted :.price
     {:text-align :center
      :color (color :meme-tile-footer)}]
    [:.title {:margin-top (em 1)
              :font-weight :bold
              :cursor :pointer}]
    [:.number-minted {:margin-top (em 0.3)}]
    [:.price
     (font :bungee)
     {:margin-top (em 0.3)}]]])
