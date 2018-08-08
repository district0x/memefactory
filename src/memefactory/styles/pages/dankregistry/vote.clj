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

(defstyles core
  [:.dank-registry-vote-page
   [:section.vote-header
    {:padding-top (em 2)
     :margin-right (em 6)
     :margin-left (em 6)}
    [:.registry-vote-header
     (panel-with-icon {:url "/assets/icons/memesubmiticon.png"
                       :color :purple})
     [:.get-dank-button
      (get-dank-button)]]]
   [:.tabs-titles
    (tabs)]])
