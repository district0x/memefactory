(ns memefactory.styles.component.ens-resolved-address
  (:require [garden.def :refer [defstyles]]
            [garden.units :refer [px, em]]
            [memefactory.styles.base.media :refer [for-media-max]]))


(defstyles core
  [:.cmp-address-wrapper
   {:display "inline"}
   [:.address
    {:text-overflow "ellipsis"
     :overflow      "hidden"}]])
