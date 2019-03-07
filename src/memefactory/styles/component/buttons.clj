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

(defn button [{:keys [color background-color before-icon after-icon width height]}]
  [:&
   (font :bungee)
   {:border-radius (em 2)
    :display "block"
    :height (or height (em 2))
    :width (or width (em 8))
    :border-style "none"
    :color (c/color color)
    :cursor :pointer
    :background-color (c/color background-color)
    :white-space :nowrap}
   [:&:disabled
    {:opacity 0.3}]])


(defn get-dank-button []
  [:&
   (font :bungee)
   {:position :relative
    ;; :bottom (em -2)
    ;; :top (em -2)
    :top (em 0)
    :height (em 4)
    :line-height (em 4)
    :right (px 0)
    :display :block
    :cursor :pointer
    :color (c/color :white)
    :background (str "url('/assets/icons/gears-button-bg-l.png') left 1em center / 40% 60% no-repeat,"
                     "url('/assets/icons/gears-button-bg-r.png') right 1em center / 40% 60% no-repeat "

                     (c/color :purple))
    :border-radius "0 0 1em 1em"
    :text-align :center
    :left (px 0)}
   [:&:after
    {:width (em 1)
     :background-position "0 .3em"
     :background-repeat "no-repeat"
     :height (em 1)
     :content "' '"
     :background-size "1em,1em"
     :margin-left (em 1)
     :display "inline-block"
     :background-image "url(/assets/icons/arrow-white-right.svg)"}]])

(defn tag []
  [:&
   {:text-transform :capitalize
    :background-color (c/color :tags-grey)
    :color :black
    :margin (em 0.3)
    :padding-left (em 2.5)
    :padding-right (em 2.5)
    :padding-top (em 0.7)
    :padding-bottom (em 0.7)
    :border "0.5px solid rgba(224, 180, 240, 0.4)"
    :border-radius (em 2)
    :font-size (em 1)
    :display :inline-block}])

(defn vote-button-icon [top]
  [:&:before {:content "''"
              :background-image "url('/assets/icons/thumb-up.svg')"
              :background-size "20px 20px"
              :display :inline-block
              :background-repeat :no-repeat
              :width (px 20)
              :position :relative
              :margin-right (px 10)
              :height (px 20)
              :top (px top)}])
