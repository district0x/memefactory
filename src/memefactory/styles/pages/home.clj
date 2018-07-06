(ns memefactory.styles.pages.home
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
     :top (em 2)
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
   (for-media-max :tablet
                  [:&
                   {:margin-right (em 1)
                    :margin-left (em 1)
                    }])
   {:margin-top (em 2)
    :margin-right (em 6)
    :margin-left (em 6)}
   [:>div
    {:background (color :meme-panel-bg)
     :box-shadow ".3em .3em 0px 0px rgba(0,0,0,0.05)"
     :position :relative}
    (border-top {:color (color :border-line)})
    (for-media-max :tablet
                   [:&
                    {:border-radius "1em 1em 1em 1em"
                     :margin-bottom (em 2)}])
    [:&.new-on-marketplace
     {:border-radius "1em 1em 1em 1em"}]]
   [:h2.title
    (font :bungee)
    (for-media-max :tablet
                   [:&
                    {:margin-top (em 4)
                     :font-size (px 19)}])
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
    (for-media-max :tablet
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
                     :color (color :white)
                     :border-radius "0 0 1em 1em"
                     :text-align :center
                     :left (px 0)}])
    [:&:after {:content "'>'"
               :margin-left (em 1)
               :color (color :pink)
               :display :inline-block}]]
   [:.icon
    (for-media-max :tablet
                   [:&
                    {:margin-right :auto
                     :margin-left :auto
                     :right 0
                     :left 0
                     }])
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
    (for-media-max :tablet
                   [:.more {:background-color (color :new-meme-icon-bg)}])
    [:.icon {:background-color (color :new-meme-icon-bg)
             :background-image (str "url('/assets/icons/newicon.png')")}]
    [:h2.title {:color (color :new-meme-icon-bg)}]]
   [:.rare-finds
    (for-media-max :tablet
                   [:.more {:background-color (color :rare-meme-icon-bg)}])
    [:.icon {:background-color (color :rare-meme-icon-bg)
             :background-image (str "url('/assets/icons/rarefindicon.png')")}]
    [:h2.title {:color (color :rare-meme-icon-bg)}]]
   [:.random-pics
    (for-media-max :tablet
                   [:.more {:background-color (color :random-meme-icon-bg)}])
    [:.icon {:background-color (color :random-meme-icon-bg)
                           :background-image (str "url('/assets/icons/randomicon.png')")}]
    [:h2.title {:color (color :random-meme-icon-bg)}]]])
