(ns memefactory.styles.component.overflow
  (:require [garden.def :refer [defstyles]]
            [garden.stylesheet :refer [at-media]]
            [clojure.string :as s]
            [garden.selectors :as sel]
            [garden.units :refer [pt px em rem]]
            [clojure.string :as str]))

(defn of-ellipsis []
  [:&
   {:white-space :nowrap
    :overflow :hidden
    :text-overflow :ellipsis}])
