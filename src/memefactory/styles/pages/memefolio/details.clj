(ns memefactory.styles.pages.memefolio.details
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
     [:.meme-image
      {:border-radius "1em 1em 1em 1em"}]
     ]]])
