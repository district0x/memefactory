(ns memefactory.styles.pages.mymemefolio
  (:require [garden.def :refer [defstyles]]
            [garden.stylesheet :refer [at-media]]
            [clojure.string :as s]
            [memefactory.styles.base.icons :refer [icons]]
            [memefactory.styles.component.buttons :refer [button]]
            [memefactory.styles.base.borders :refer [border-top border-bottom]]
            [memefactory.styles.base.colors :refer [color]]
            [memefactory.styles.base.fonts :refer [font]]
            [memefactory.styles.base.media :refer [for-media-min for-media-max]]
            [memefactory.styles.component.search :refer [search-panel]]
            [memefactory.styles.component.panels :refer [tabs]]
            [garden.selectors :as sel]
            [garden.units :refer [pt px em rem]]
            [clojure.string :as str]))

(defstyles core
  [:.memefolio-page
   (search-panel {:background-panel-image "/assets/icons/search-background.png"
                  :color :mymemefolio-green
                  :icon "/assets/icons/mymemefolio-green.png"})

   [:.total
    {:position :relative
     :margin-right (em 6)
     :margin-left (em 6)
     :height 1}
    [">div"
     {:right (em 0)
      :font-size (em 0.9)
      :position :absolute
      :top (em -2)}]]
   [:section.stats
    {:display :flex
     :margin-top 0
     :padding-top (em 0)
     :padding-bottom (em 0)
     :margin-right (em 6)
     :margin-left (em 6)
     :box-shadow ".3em .3em 0px 0px rgba(0,0,0,0.05)"
     :background-color (color :yellow)
     :border-radius "1em 1em 0em 0em"}
    #_[">*"
     {:padding-top (em 1)
      :padding-bottom (em 1)}]
    [">.rank"
     {:width "100%"}]
    [".rank>.stats>.rank"
     (font :bungee)
     {:color (color :purple)
      :border-radius "1em 0em 0em 0em"
      :background-color (color "#ffd800")
      :padding-right (em 1)
      :padding-left (em 1)
      :display :table-cell
      :font-size (em 1)}
     [:&.big
      {:line-height (em 6)}]]
    [:.stats
     {:display :flex
      :line-height (em 3)}
     [:&.]
     [:.unique-memes :.largest-buy
      {:margin-right (em 1)
       :margin-left (em 1)}]
     [:.curator
      {:display :grid
       :flex-grow 1
       :grid-cols "100%"}
      [">div"
       {:display :flex}
       [:.label
        {:color (color :pink)}
        (font :bungee)]
       [">*"
        {:margin-right (em 1)
         :line-height (em 2)
         :margin-left (em 1)}
        [:b {:margin-right (em 0.2)}]]]]]]
   [:.tiles
    {:margin-top 0
     :padding-top (em 2)
     :padding-bottom (em 2)
     :margin-right (em 6)
     :margin-left (em 6)
     :background-color (color :meme-panel-bg)
     :box-shadow ".3em .3em 0px 0px rgba(0,0,0,0.05)"
     :border-radius "1em 1em 1em 1em"}
    [">div>div>div"
     {:display :flex
      :flex-wrap :wrap
      :justify-content :space-evenly}
     [:.footer {:text-align :center}]
     [:.issue-form {:text-align :center
                    :margin-top (em 1)}
      [:.field {:display :grid
                :grid-template-columns "50% 50%"
                :margin-left "25%"
                :margin-right "25%"
                :overflow :hidden
                :width "60%"
                :height (em 2.5)
                :border-radius (em 2)}
       [:button 
        (font :bungee)
        {:display "block"
         :bottom (em 2)
         :border-radius 0
         :height (em 3)
         :width "100%"
         :border-style "none"
         :color (color :white)
         :background-color (color :purple)}
        [:&:disabled
         {:opacity 0.3}]]
       [:input {:background-color (color :search-input-bg)
                :text-align :center
                :height (em 3)}
        [:.help-block {:display :none}]]]]]]
   [:section.tabs
    (tabs)]])
