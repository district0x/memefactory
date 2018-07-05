(ns memefactory.styles.pages.home
  (:require [garden.def :refer [defstyles]]
            [garden.stylesheet :refer [at-media]]
            [clojure.string :as s]
            [memefactory.styles.base.icons :refer [icons]]
            [memefactory.styles.base.borders :refer [border-top]]
            [memefactory.styles.base.colors :refer [color]]
            [memefactory.styles.base.fonts :refer [font]]
            [garden.selectors :as sel]
            [garden.units :refer [pt px em rem]]
            [clojure.string :as str]))


(defstyles core
  [:p.inspired
   (font :filson)
   {:color (color :pink)
    :position :relative
    :margin-right :auto
    :margin-left :auto
    :padding-top (em 7)
    :text-align :center
    :width "50%"}
   [:&:before
    {:content "''"
     :background-size [(em 4) (em 4)]
     :background-repeat :no-repeat
     :bottom (em 3)
     ;; :center (rem 0)
     :position :absolute
     :margin-right :auto
     :margin-left :auto
     :right 0
     :left 0
     :height (em 4)
     :width (em 4)
     :background-image (str "url('/assets/icons/mf.png')")}]]

  [:.meme-highlights
   [:>div
    {:position :relative}
    (border-top {:color (color :border-line)})]
   {:background (color :meme-panel-bg)
    :border-radius (em 1)
    :box-shadow ".3em .3em 0px 0px rgba(0,0,0,0.05)"
    :margin-top (em 2)
    :margin-right (em 6)
    :margin-left (em 6)}
   [:h2.title
    (font :bungee)
    {:white-space :nowrap
     :position :relative
     ;; :color (color :section-caption)
     :font-size (px 25)
     :margin-top (em 0.3)
     :margin-bottom (em 0.1)
     :text-align :center}]
   [:h3.title
    {:white-space :nowrap
     :margin-top (em 0.1)
     :position :relative
     :color (color :section-subcaption)
     :font-size (px 15)
     :text-align :center}]
   [:a.more {:position :absolute
             :top (em 2)
             :color (color :section-subcaption)
             :right (em 2)}
    [:&:after {:content "'>'"
               :margin-left (em 1)
               :color (color :pink)
               :display :inline-block}]]
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
   [:.new-on-marketplace
    [:.icon {:background-color (color :new-meme-icon-bg)
                                  :background-image (str "url('/assets/icons/newicon.png')")}]
    [:h2.title {:color (color :new-meme-icon-bg)}]]
   [:.rare-finds
    [:.icon {:background-color (color :rare-meme-icon-bg)
                          :background-image (str "url('/assets/icons/rarefindicon.png')")}]
    [:h2.title {:color (color :rare-meme-icon-bg)}]]
   [:.random-pics [:.icon {:background-color (color :random-meme-icon-bg)
                           :background-image (str "url('/assets/icons/randomicon.png')")}]
    [:h2.title {:color (color :random-meme-icon-bg)}]]])
