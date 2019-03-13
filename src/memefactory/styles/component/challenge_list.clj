(ns memefactory.styles.component.challenge-list
  (:require
   [garden.def :refer [defstyles]]
   [garden.stylesheet :refer [at-media]]
   [clojure.string :as s]
   [memefactory.styles.base.colors :refer [color]]
   [memefactory.styles.base.media :refer [for-media-min for-media-max]]
   [memefactory.styles.base.fonts :refer [font]]
   [memefactory.styles.base.fonts :refer [font]]
   [memefactory.styles.component.buttons :refer [button tag]]
   [garden.units :refer [px em pt]]))


(defstyles core
  [:.challenges.panel
    [:.spinner-container {:width (px 900)
                         :height (px 500)}
    [:.spinner-outer {:margin-left :auto
                      :margin-right :auto
                      :padding-top (em 12)}]]


   #_[:.controls {:width (em 11)
                :margin-left :auto
                :margin-right 0}]
   [:.controls {:display :block
                :margin-top (em 4)
                :margin-left :auto
                :margin-right 0
                :width (em 11)
                :right (em 0)
                :height 0
                :top (em -4)
                :position :relative}
    (for-media-max :tablet
                   [:&
                    {:margin-top (em 1)}])
    [:.help-block {:display :none}]
    (for-media-max :tablet
                   [:&
                    {:display :inline-block
                     :height (em 1)
                     :padding-top (em 1)
                     :width "100% !important"
                     :top (em -1)}])]
   [:.challenge
    {:display :grid
     :grid-template "'info image action'"
     :grid-template-columns "1fr 1fr 1fr"
     :grid-gap (em 2)
     :background-color :white
     :margin-bottom (em 1.5)
     :border-radius (em 0.6)
     :box-shadow ".3em .3em 0px 0px rgba(0,0,0,0.05)"
     :padding (em 2.4)}
    (for-media-max :computer
                   [:& {:grid-template "'image' 'info' 'action'"}])
    (for-media-max :large
                   [:& {:padding (em 1.4)}])
    [:.info {:overflow :hidden
             :grid-area :info}
     [:h2
      {:color (color :purple)
       :cursor :pointer
       :text-transform :uppercase
       :word-wrap :break-word}
      (font :bungee)
      (for-media-max :computer
                     [:& {:text-align :center}])]
     [:h3 {:color (color :menu-text)
           :font-weight :bold
           :font-size (em 0.8)
           :margin 0}
      [:&.challenger {:margin-top (em 1)}]]
     [:ol
      {:list-style-type :none
       :font-size (em 0.8)
       :padding 0
       :margin-top (em 0.4)
       :color (color :menu-text)}
      [:&.tags ;; TODO refactor out tags from here
       {:margin-top (em 2)}
       [:li
        (tag)]]
      [:li {:display :flex}
       [:span {:overflow :hidden
               :white-space :nowrap
               :text-overflow :ellipsis
               :display :inline-block
               :max-width (em 18)
               :margin-left (em 0.2)}]
       [:span.address.creator {:cursor :pointer}]]]
     [:span.challenge-comment {:font-style :italic
                               :color (color :pink)}]]
    [:div.meme-tile {:display :grid
                     :grid-area :image
                     :justify-items :center}
     [:.meme-card {:position :relative}]]
    [:.action {:margin :auto
               :grid-area :action}
     [:.period-ended {:color (color :menu-text)
                      :display :inline-block
                      :margin (em 0.5)}]]]])
