(ns memefactory.styles.pages.dankregistry
  (:require [memefactory.styles.pages.dankregistry.index :as index]
            [memefactory.styles.pages.dankregistry.submit :as submit]
            [memefactory.styles.pages.dankregistry.vote :as vote]))

(defstyles core
  index/core
  submit/core
  vote/core)
