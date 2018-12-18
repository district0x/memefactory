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
   [memefactory.styles.component.overflow :refer [of-ellipsis]]
   [garden.units :refer [px em pt]]))

(def bar-height (px 50))
(def card-aspect-ratio 1.5)
(def card-width 290)
(def card-height (int (* card-aspect-ratio card-width)))

(defstyles core
  [:.container
   {:transform-style :preserve-3d
    :margin-right :auto
    :margin-left :auto
    :left 0
    :right 0
    :width (px card-width)
    :height (px card-height)
    :perspective (px 10000)}
   [:.meme-card
    {:cursor :pointer}
    [:&.front :&.back
     {:transition "transform .7s cubic-bezier(0.4, 0.2, 0.2, 1)"}]
    [:&.front
     {:text-align :center
      ;; :backface-visibility :hidden
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
      :pointer-events :none
      :transform "rotateY(-180deg)"}]
    [:&.back
     {:transform-style :preserve-3d
      :transform "rotateY(0deg)"}]]]
  [:.meme-card
   {:position :absolute
    :background-color (color "gray")
    :width (px card-width)
    :height (px card-height)
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
    {:max-width (px card-width)
     :height (px card-height)}]
   [:.overlay {:position :absolute
               ;; :background-color (color :meme-bg)
               :background (str "linear-gradient(to bottom, " (color :meme-bg) " 0%," (color :meme-bg) " 80%," (color :meme-bg-bottom) " 80%," (color :meme-bg-bottom) " 100%)")
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
     [:hr
      {:margin (em 1)
       :border 0
       :height 0
       :border-top "1px solid rgba(0, 0, 0, 0.1)"
       :border-bottom "1px solid rgba(255, 255, 255, 0.3)"
       }]
     [:.description {:font-style :italic}]
     [:button
      {:right 0
       :left 0
       :margin-right :auto
       :margin-left :auto
       :position "absolute"
       :bottom (em 1.8)}
      (button {:background-color :meme-buy-button
               :color :light-light-grey
               :width (em 9)
               :height (em 3)})
      [:&.buy:after {:content "' '";;"&#8594;" "' →'"
                 :height (em 1)
                 :width (em 1)
                 :display :inline-block
                 :background-position "0 0.3em"
                 :margin-left (em 1)
                 :background-size "1em, 1em"
                 :background-repeat "no-repeat"
                 :background-image "url(/assets/icons/arrow-white-right.svg)"}]]
     [:.meme-data {:text-align :center
                   :padding-left (em 1.2)
                   :padding-right (em 1.2)
                   :font-size (pt 13)
                   :line-height (em 1.8)
                   :list-style :none}
      [:&:before {:content "''"
                  :margin-top (em 2)
                  :height (em 3)
                  :width (em 3)
                  :display :block
                  :margin-right :auto
                  :margin-left :auto
                  :right 0
                  :left 0
                  :background-size "3em, 3em"
                  :background-repeat "no-repeat"
                  :background-image "url(/assets/icons/mf-logo.svg)"}]
      [:>li
       (of-ellipsis)
       {:margin-top (em 0.3)
        :font-size (em 0.7)
        :padding-right (em 0.5)
        :padding-left (em 0.5)
        :margin-bottom (em 0.3)}]
      [:label {:font-weight :bold
               :padding-right (em 0.3)}]
      [:span {:overflow :hidden
              :text-overflow :ellipsis
              :white-space :nowrap}]]]]]
  [:.compact-tile
   {:background (color :meme-panel-bg)
    :width (px card-width)
    :margin-top (em 1)
    :margin-bottom (em 1)
    ;; :box-shadow "0 0 50px 20px rgba(0, 0, 0, 0.04)"
    :display :block
    :position :relative
    :text-align :center}
   [:.footer
    {:position :relative
     :bottom 0
     :cursor :pointer
     :line-height (em 1.2)
     :font-size (em 0.9)}
    [:.token-id :.title {:display :inline-block}]
    [:.token-id {:padding-right (em 0.3)}]
    [:.title :.number-minted :.price :.token-id
     {:text-align :center
      :color (color :meme-tile-footer)}]
    [:.title {:margin-top (em 1)
              :font-weight :bold
              :cursor :pointer}]
    [:.number-minted {:margin-top (em 0.3)}]
    [:.price
     (font :bungee)
     {:margin-top (em 0.3)}]]])
