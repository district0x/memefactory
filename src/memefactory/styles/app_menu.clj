(ns memefactory.styles.app-menu
  (:require [garden.def :refer [defstyles]]
            [garden.stylesheet :refer [at-media]]
            [clojure.string :as s]
            [memefactory.styles.base.colors :refer [color]]
            [garden.units :refer [px]]))

(def menu-gutter (px 15))
(defstyles core
  [:.app-container
   [:.app-menu
    {:overflow-x :hidden
     :overflow-y :scroll
     :background (color "white")}
    [:div.item
     {:display :flex
      :align-items :center
      :padding-top menu-gutter
      :padding-bottom menu-gutter}]]])
