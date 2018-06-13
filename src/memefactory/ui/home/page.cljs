(ns memefactory.ui.home.page
  (:require
   [district.ui.component.page :refer [page]]
   [district.ui.graphql.subs :as gql]
   [memefactory.ui.marketplace.events :as mk-events]
   [memefactory.ui.components.app-layout :refer [app-layout]]
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as r]
   [react-infinite]
   [memefactory.shared.utils :as shared-utils]))

(defmethod page :route/home [] 
  (let [search-atom (r/atom {:term ""})]
    (fn []
      [app-layout
       {:meta {:title "MemeFactory"
               :description "Description"}
        :search-atom search-atom}
       [:div.home
        [:img.logo]
        [:p "Inspired by the work of Simon de la Rouviere and his Curation Markets design, the third district to be deployed to dthe district0x."]
        [:div.new-on-marketplace
         [:div.header
          [:img]
          [:div.middle
           [:h2.title "New On Marketplace"]
           [:h3.title "Lorem ipsum ..."]]
          [:a "See More"]]]
        [:div.rare-finds
         [:div.header
          [:img]
          [:div.middle
           [:h2.title "Rare Finds"]
           [:h3.title "Lorem ipsum ..."]]
          [:a "See More"]]]
        [:div.random-pics
         [:div.header
          [:img]
          [:div.middle
           [:h2.title "Random Picks"]
           [:h3.title "Lorem ipsum ..."]]
          [:a "See More"]]]]])))


