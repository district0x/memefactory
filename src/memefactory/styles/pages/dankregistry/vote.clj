(ns memefactory.styles.pages.dankregistry.vote
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
            [memefactory.styles.component.panels :refer [panel-with-icon tabs]]
            [memefactory.styles.component.buttons :refer [get-dank-button button]]
            [clojure.string :as str]))

(defn vote-button-icon []
  [:&:before {:content "''" 
              :background-image "url('/assets/icons/dankregistry.png')"
              :background-size "20px 20px"
              :display :inline-block
              :background-repeat :no-repeat
              :width (px 20)
              :position :relative
              :margin-right (px 10)
              :height (px 20)}])

(defstyles core
  [:.dank-registry-vote-page
   [:section.vote-header
    {:padding-top (em 2)
     :margin-right (em 6)
     :margin-left (em 6)}
    (for-media-max :large
                   [:&
                    {:margin-right (em 2)
                     :margin-left (em 2)}])
    [:.registry-vote-header
     (panel-with-icon {:url "/assets/icons/memesubmiticon.png"
                       :color :purple})
     [:.get-dank-button
      (get-dank-button)]]]
   [:.tabs-titles
    (tabs)]
   [:.vote {:display :grid
            :margin-left (em 6)
            :margin-right (em 6)}
    (for-media-max :large
                   [:&
                    {:margin-right (em 2)
                     :margin-left (em 2)}])
    [:.vote-input {:display :grid
                   :grid-template-columns "80% 20%"
                   :border-bottom "1px solid"
                   :margin-bottom (em 1)}
     [:.help-block {:display :none}]]
    [:.vote-dank
     [:button
      {:margin-bottom (em 2)}
      (button {:background-color :rare-meme-icon-bg
               :color :violet
               :height (em 3)
               :width "100%"})
      (vote-button-icon)]]
    [:.vote-stank
     [:button 
      (button {:background-color :random-meme-icon-bg
               :color :violet
               :height (em 3)
               :width "100%"})
      (vote-button-icon)
      [:&:before {:transform "scaleX(-1) scaleY(-1)"}]
      ]]]
   [:.reveal
    {:text-align :center}
    [:img {:width (em 7)}]
    [:button
     {:margin-top (em 2)}
     (button {:background-color :purple
              :color :white
              :height (em 3)
              :width (em 14)}) ]]
   [:.collect-reward
    {:display :grid
     :justify-items :center}
    [:.vote-info {:list-style :none
                  :color (color :menu-text)}]
    [:button
     {:margin-top (em 2)}
     (button {:background-color :purple
              :color :white
              :height (em 3)
              :width (em 14)}) ]]]) 
