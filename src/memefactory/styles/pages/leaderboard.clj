(ns memefactory.styles.pages.leaderboard
  (:require
   [garden.def :refer [defstyles]]
   [memefactory.styles.pages.leaderboard.index :as index]
   [memefactory.styles.pages.leaderboard.curators :as curators]
   [memefactory.styles.pages.leaderboard.collectors :as collectors]
   [memefactory.styles.pages.leaderboard.creators :as creators]))

(defstyles core
  index/core
  curators/core
  collectors/core
  creators/core)
