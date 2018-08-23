(ns memefactory.styles.pages.memefolio.details
  (:require [garden.def :refer [defstyles]]
            [garden.stylesheet :refer [at-media]]
            [clojure.string :as s]
            [memefactory.styles.base.icons :refer [icons]]
            [memefactory.styles.component.buttons :refer [button tag]]
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
  [:.meme-detail-page
   {;;:display "grid"
    ;; :grid-template-areas (str
    ;;                       "'image image image rank rank rank'"
    ;;                       "'history history history history history history'"
    ;;                       "'challenge challenge challenge challenge challenge challenge'"
    ;;                       "'related related related related related related'")
    }
   [:section.meme-detail
    {:padding (em 3)}
    [:.meme-info
     {:background (color :meme-panel-bg)
      :box-shadow ".3em .3em 0px 0px rgba(0,0,0,0.05)"
      :border-radius "1em 1em 1em 1em"
      :display :grid
      :grid-template-columns "34% 66%"
      :grid-template-rows "100%"
      :position :relative}
     [:.meme-num
      (font :bungee)
      {:position "absolute"
       :display "block"
       :color (color :purple)
       :left (em 3.1)
       :padding (em 1)
       :font-size (em 1.1)
       :border-radius "0em 0em 1em 1em"
       :background-color (color :yellow)}]
     [:.meme-image
      {:padding (em 1)
       :border-radius "1em 1em 1em 1em"}]
     [:.registry
      {:padding (em 1)}
      [:h1
       (font :bungee)
       {:color (color :purple)}]
      [:.status
       {:display :flex
        :line-height (em 1)}
       [:&:before
        {:content "''"
         :height (em 2.2)
         :display :inline-block
         :width (em 2.2)
         :transform "scale(0.5, 0.5)"
         :background-repeat :no-repeat
         :background-position "center center";
         :background-image (str "url('/assets/icons/inregistry-icon.png')")}]
       [:label
        {:line-height (em 2.3)
         :font-weight :bold
         :color (color :menu-text)}]]
      [:.description, :.text
       {:color (color :menu-text)}]
      [:.tags
       [:button (tag)]]
      [:.buttons
       {:display :flex}
       [:button.search
        (button {:color :meme-buy-button})
        {:min-width (em 18)
         :margin (em 1)}
        [:&.marketplace {:background-color (color :purple)
                         :color (color :white)}]
        [:&.memefolio {:background-color (color :pink)
                         :color (color :white)}]]]]]]])
