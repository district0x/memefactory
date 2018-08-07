(ns memefactory.styles.pages.leaderboard
  (:require [memefactory.styles.pages.leaderboard.index :as index]
            [garden.def :refer [defstyles]]
            [memefactory.styles.pages.leaderboard.curators :as curators]
            [memefactory.styles.pages.leaderboard.collectors :as collectors]))

(defstyles core
  index/core
  curators/core
  curators/core)
