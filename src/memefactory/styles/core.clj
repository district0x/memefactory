(ns memefactory.styles.core
  (:require [garden.def :refer [defstyles]]
            [garden.units :refer [px]]
            [memefactory.styles.app-layout :as app-layout]
            [memefactory.styles.component.app-bar :as app-bar]
            [memefactory.styles.component.main-content :as main-content]
            [memefactory.styles.component.compact-tile :as compact-tile]
            [memefactory.styles.component.form :as form]
            [memefactory.styles.container.tiles :as tiles]
            [memefactory.styles.app-menu :as app-menu]
            [memefactory.styles.pages.home :as page.home]
            [memefactory.styles.pages.marketplace :as page.marketplace]
            [memefactory.styles.pages.mymemefolio :as page.memefolio]
            [memefactory.styles.pages.dankregistry :as page.dankregistry]
            [memefactory.styles.pages.dankregistry.challenge :as page.dankregistry.challenge]
            [memefactory.styles.pages.dankregistry.vote :as page.dankregistry.vote]
            [memefactory.styles.pages.leaderboard :as page.leaderboard]
            [memefactory.styles.component.challenge-list :as challenge-list]))

(defstyles main
  app-layout/core
  app-menu/core
  main-content/core
  app-bar/core
  tiles/core
  compact-tile/core
  page.home/core
  page.marketplace/core
  form/core
  page.memefolio/core
  page.dankregistry/core
  page.dankregistry.challenge/core
  page.dankregistry.vote/core
  page.leaderboard/core
  challenge-list/core)
