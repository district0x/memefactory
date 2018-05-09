(ns memefactory.styles.app-menu
  (:require [garden.def :refer [defstyles]]
            [garden.stylesheet :refer [at-media]]
            [clojure.string :as s]
            [memefactory.styles.base.colors :refer [color]]
            [garden.units :refer [px]]))

(def menu-gutter (px 15))
(def menu-item )
(defstyles core
  [:.app-container
   [:.app-menu
    {:overflow-x :hidden
     :overflow-y :scroll
     :background (color "white")}
    [:.ui.link.list
     [:.item
      {:display :flex
       :align-items :center
       :padding-top menu-gutter
       :padding-bottom menu-gutter}
      [:&:before
       {:font-family "Icons"
        :display :block
        :height (px 40)
        :width (px 40)
        :float :left
        :color (color "green")}]
      [:&.dankregistry:before
       {:content "\"\\f0c2\""}]
      [:&.marketplace:before
       {:content "\"\\f2a3\""}]]]]])
