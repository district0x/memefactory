(ns memefactory.styles.pages.home
  (:require [garden.def :refer [defstyles]]
            [garden.stylesheet :refer [at-media]]
            [clojure.string :as s]
            [memefactory.styles.base.icons :refer [icons]]
            [memefactory.styles.base.borders :refer [border-top]]
            [memefactory.styles.base.colors :refer [color]]
            [garden.selectors :as sel]
            [garden.units :refer [pt px em rem]]
            [clojure.string :as str]))


(defstyles core
  [:.meme-highlights
   [:>div
    (border-top {:color (color :border-line)})]
   {:background (color :meme-panel-bg)
    :border-radius (em 1)
    :box-shadow ".3em .3em 0px 0px rgba(0,0,0,0.05)"
    :margin (em 3)}
   [:.icon
    {:display :block
     :background-size [(em 4) (em 4)]
     :background-repeat :no-repeat
     :box-shadow ".3em .3em 0px 0px rgba(0,0,0,0.05)"
     :border-radius "0em 0em 1em 1em"
     :margin-left (em 2)
     :margin-top (em 0)
     :position :absolute
     :height (em 4)
     :width (em 4)}]
   [:.new-on-marketplace [:.icon {:background-color (color :new-meme-icon-bg)
                                  :background-image (str "url('/assets/icons/newicon.png')")}]]
   [:.rare-finds [:.icon {:background-color (color :rare-meme-icon-bg)
                          :background-image (str "url('/assets/icons/rarefindicon.png')")}]]
   [:.random-pics [:.icon {:background-color (color :random-meme-icon-bg)
                           :background-image (str "url('/assets/icons/randomicon.png')")}]]
   ])
