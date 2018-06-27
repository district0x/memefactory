(ns memefactory.styles.core
  (:require [garden.def :refer [defstyles]]
            [garden.units :refer [px]]
            [memefactory.styles.app-layout :as app-layout]
            [memefactory.styles.component.app-bar :as app-bar]
            [memefactory.styles.component.main-content :as main-content]
            [memefactory.styles.component.compact-tile :as compact-tile]
            [memefactory.styles.container.tiles :as tiles]
            [memefactory.styles.app-menu :as app-menu]

            [memefactory.styles.pages.home :as page.home]
            ))

;; didn't knew where to put this Mike
(defstyles inputs
  [:.dropzone
   {:width (px 200)
    :height (px 200)
    :background-color :grey}
   [:img {:width (px 200)}]])

(defstyles main
  app-layout/core
  app-menu/core
  main-content/core
  app-bar/core
  tiles/core
  compact-tile/core
  page.home/core
  inputs)
