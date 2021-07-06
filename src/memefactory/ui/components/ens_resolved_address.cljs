(ns memefactory.ui.components.ens-resolved-address
  (:require
    [reagent.core :as r]
    [cljsjs.ens-resolved-address]))

(def ens-resolved-address (r/adapt-react-class js/EnsResolvedAddress))
