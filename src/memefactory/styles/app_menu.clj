(ns memefactory.styles.app-menu
  (:require [garden.def :refer [defstyles]]
            [garden.stylesheet :refer [at-media]]
            [clojure.string :as s]
            [memefactory.styles.base.icons :refer [icons]]
            [memefactory.styles.base.borders :refer [border-top]]
            [memefactory.styles.base.colors :refer [color]]
            [garden.selectors :as sel]
            [garden.units :refer [px]]))

(def menu-gutter (px 15))

(defstyles core
  [:.app-container
   [:.app-menu
    {:overflow-x :hidden
     :overflow-y :scroll
     :width (px 190)
     :background (color :violet)}
    [(sel/> :.menu-content :.node :.node-content :.item)
     (border-top {:color (color :light-violet)})]
    [:.node
     [:.item
      {:display :flex
       :align-items :center
       :padding-top menu-gutter
       :padding-bottom menu-gutter}
      [:a
       {:color (color :light-grey)}
       [:&:hover
        {:color (color :white)}]]
      [:&:before
       {:font-family "Icons"
        :display :block
        :height (px 40)
        :width (px 40)
        :float :left
        :color (color "green")}]
      [:&.dankregistry:before
       {:content (icons :eye)}]
      [:&.marketplace:before
       {:content (icons :dollar-circle)}]]]]])
