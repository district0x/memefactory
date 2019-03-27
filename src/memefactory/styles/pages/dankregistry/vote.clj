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
            [memefactory.styles.component.buttons :refer [get-dank-button button vote-button-icon]]
            [clojure.string :as str]))



(defstyles core
  [:.dank-registry-vote-page
   [:section.vote-header
    [:.registry-vote-header
     (panel-with-icon {:url "/assets/icons/memesubmiticon.svg"
                       :color :blue})
     [:.get-dank-button
      (get-dank-button)]]]
   [:section.challenges
    {:max-width (px 985)
     :margin-left :auto
     :margin-right :auto}

    [:.challenges
     {:padding-left (px 0)
      :padding-right (px 0)}]]
   [:.tabs-titles
    (tabs)
    [:& {:margin-top (em 3)}]]
   [:.selected-tab-body
    [:.challenges.panel
     {:color (color :menu-text)}
     [:.controls {:display :block
                  :margin-top (em 0)
                  :right (em 0)
                  :height 0
                  :top (em -4)
                  :width (em 14)
                  :position :relative}

      [:.help-block {:display :none}]
      (for-media-max :tablet
                     [:&
                      {:display :inline-block
                       :height (em 1)
                       :padding-top (em 1)
                       :width "100% !important"
                       :top (em -1)}])]]]
   [:.vote {:display :grid
            :margin-left (em 4)
            :margin-right (em 4)}
    (for-media-max :large
                   [:&
                    {:margin-right (em 2)
                     :margin-left (em 2)}])
    [:.vote-input {:display :grid
                   :height (px 30)
                   :grid-template-columns "80% 20%"
                   :border-bottom "1px solid"
                   :margin-bottom (em 1)}
     [:span {:margin-top (px 8)}]
     [:.help-block {:display :none}]]
    [:.vote-dank
     [:button
      {:margin-bottom (em 2)}
      (button {:background-color :rare-meme-icon-bg
               :color :violet
               :height (em 3)
               :width "100%"})
      (vote-button-icon 2)
      {:white-space :nowrap
       :padding-right (em 1)
       :padding-bottom (em 0.4)
       :padding-left (em 1)}]]
    [:.vote-stank
     [:button
      (button {:background-color :random-meme-icon-bg
               :color :violet
               :height (em 3)
               :width "100%"})
      (vote-button-icon 6)
      {:white-space :nowrap
       :padding-right (em 1)
       :padding-bottom (em 0.4)
       :padding-left (em 1)}
      [:&:before {:transform "scaleX(-1) scaleY(-1)"}]]]
    [:p.max-vote-tokens {:text-align :center
                         :margin-top (em 1)
                         :margin-left (em 0.5)
                         :margin-right (em 0.5)
                         :font-size (px 12)}]
    [:p.token-return {:text-align :center
                      :font-size (px 12)
                      :margin-left (em 0.5)
                      :margin-right (em 0.5)}]]
   [:.reveal
    {:text-align :center}
    [:img {:width (em 7)}]
    [:.button-wrapper {:display :block}
     [:button
      {:margin-top (em 2)}
      (button {:background-color :purple
               :color :white
               :height (em 3)
               :width (em 14)})]]
    [:.no-reveal-info {:color :red
                       :margin-top (em 1)}]]
   [:.collect-reward
    {:display :grid
     :justify-items :center}
    [:.vote-info {:list-style :none
                  :text-align :center
                  :padding-left (px 0)}]
    [:button
     {:margin-top (em 1)}
     (button {:background-color :purple
              :color :white
              :height (em 2.5)
              :width (em 13)})
     [:&.collect-reward {:background-color (color :pink)}]]]])
