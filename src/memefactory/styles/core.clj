(ns memefactory.styles.core
  (:require [garden.def :refer [defstyles]]
            [memefactory.styles.animations :as animations]
            [memefactory.styles.app-layout :as app-layout]
            [memefactory.styles.app-menu :as app-menu]
            [memefactory.styles.component.account-balances :as account-balances]
            [memefactory.styles.component.app-bar :as app-bar]
            [memefactory.styles.component.challenge-list :as challenge-list]
            [memefactory.styles.component.compact-tile :as compact-tile]
            [memefactory.styles.component.discord :as discord]
            [memefactory.styles.component.form :as form]
            [memefactory.styles.component.main-content :as main-content]
            [memefactory.styles.component.selling-panel :as selling-panel]
            [memefactory.styles.component.spinner :as spinner]
            [memefactory.styles.component.share-buttons :as share-buttons]
            [memefactory.styles.container.tiles :as tiles]
            [memefactory.styles.pages.about :as page.about]
            [memefactory.styles.pages.bridge :as page.bridge]
            [memefactory.styles.pages.dankregistry :as page.dankregistry]
            [memefactory.styles.pages.dankregistry.challenge
             :as
             page.dankregistry.challenge]
            [memefactory.styles.pages.dankregistry.vote :as page.dankregistry.vote]
            [memefactory.styles.pages.get-dank :as page.get-dank]
            [memefactory.styles.pages.home :as page.home]
            [memefactory.styles.pages.how-it-works :as page.how-it-works]
            [memefactory.styles.pages.leaderboard :as page.leaderboard]
            [memefactory.styles.pages.marketplace :as page.marketplace]
            [memefactory.styles.pages.my-settings :as page.my-settings]
            [memefactory.styles.pages.mymemefolio :as page.memefolio]
            [memefactory.styles.pages.param-changes :as page.param-changes]
            [memefactory.styles.pages.privacy-policy :as page.privacy-policy]
            [memefactory.styles.pages.terms :as page.terms]))

(defstyles main
  animations/core
  app-layout/core
  app-menu/core
  main-content/core
  app-bar/core
  tiles/core
  compact-tile/core
  page.home/core
  page.marketplace/core
  form/core
  spinner/core
  discord/core
  share-buttons/core
  account-balances/core
  page.memefolio/core
  page.dankregistry/core
  page.dankregistry.challenge/core
  page.dankregistry.vote/core
  page.leaderboard/core
  page.my-settings/core
  page.get-dank/core
  page.about/core
  page.bridge/core
  page.privacy-policy/core
  page.param-changes/core
  page.terms/core
  page.how-it-works/core
  challenge-list/core
  selling-panel/core)
