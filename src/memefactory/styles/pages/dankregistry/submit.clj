(ns memefactory.styles.pages.dankregistry.submit
  (:require [garden.def :refer [defstyles]]
            [garden.stylesheet :refer [at-media]]
            [clojure.string :as s]
            [memefactory.styles.base.icons :refer [icons]]
            [memefactory.styles.base.borders :refer [border-top]]
            [memefactory.styles.base.colors :refer [color]]
            [memefactory.styles.base.fonts :refer [font]]
            [memefactory.styles.base.media :refer [for-media-min for-media-max]]
            [garden.selectors :as sel]
            [garden.units :refer [pt px em rem]]
            [memefactory.styles.component.panels :refer [panel-with-icon]]
            [clojure.string :as str]))

(defstyles core
  [:.dank-registry-submit-page
   [:.submit
    {:padding-top (em 2)
     :margin-right (em 6)
     :margin-left (em 6)}
    [:.submit-info
     (panel-with-icon {:url "/assets/icons/memesubmiticon.png"
                       :color :purple})
     [:.get-dank-button
      (font :bungee)
      {:position :relative
       ;; :bottom (em -2)
       ;; :top (em -2)
       :top (em 0)
       :height (em 4)
       :line-height (em 4)
       :right (px 0)
       :display :block
       :color (color :white)
       ;; :background-color (color :purple)
       ;; :background-image (str "url('/assets/icons/gears-button-bg.png')")
       ;; :background-size :contain
       ;; :background-repeat :repeat-x
       :background (str "url('/assets/icons/gears-button-bg-r.png') left 1em center / 40% 60% no-repeat,"
                        "url('/assets/icons/gears-button-bg-l.png') right 1em center / 40% 60% no-repeat "

                        (color :purple))
       :border-radius "0 0 1em 1em"
       :text-align :center
       :left (px 0)}]]]])
