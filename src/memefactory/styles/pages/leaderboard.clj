(ns memefactory.styles.pages.leaderboard
  (:require [memefactory.styles.pages.leaderboard.index :as index]
            [garden.def :refer [defstyles]]
            [memefactory.styles.pages.leaderboard.curators :as curators]))

(defstyles core
  index/core
  curators/core)
