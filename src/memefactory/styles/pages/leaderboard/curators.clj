(ns memefactory.styles.pages.leaderboard.curators
  (:require [garden.def :refer [defstyles]]
            [garden.stylesheet :refer [at-media]]
            [clojure.string :as s]
            [memefactory.styles.base.icons :refer [icons]]
            [memefactory.styles.base.borders :refer [border-top border-bottom]]
            [memefactory.styles.base.colors :refer [color]]
            [memefactory.styles.base.fonts :refer [font]]
            [memefactory.styles.base.media :refer [for-media-min for-media-max]]
            [memefactory.styles.component.search :refer [search-panel]]
            [garden.selectors :as sel]
            [garden.units :refer [pt px em rem]]
            [memefactory.styles.component.panels :refer [panel-with-icon]]
            [clojure.string :as str]))

(defstyles core
  [:.leaderboard-curators-page
   [:section.curators
    {:padding-top (em 2)
     :margin-right (em 6)
     :margin-left (em 6)}
    [:.curators-panel
     (panel-with-icon {:url "/assets/icons/memesubmiticon.png"
                       :color :purple})]]])
