(ns memefactory.ui.how.page
  (:require [memefactory.ui.components.app-layout :refer [app-layout]]
            [district.ui.component.page :refer [page]]))

(defmethod page :route.how-it-works/index []
  [app-layout
   {:meta {:title "MemeFactory - How It Works"
           :description "MemeFactory is decentralized registry and marketplace for the creation, exchange, and collection of provably rare digital assets."}}
   [:div.how-it-works]])
