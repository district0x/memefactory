(ns memefactory.ui.about.page
  (:require [memefactory.ui.components.app-layout :refer [app-layout]]
            [district.ui.component.page :refer [page]]))

(defmethod page :route.about/index []
  [app-layout
   {:meta {:title "MemeFactory - About"
           :description "MemeFactory is decentralized registry and marketplace for the creation, exchange, and collection of provably rare digital assets."}}
   [:div.about]])
