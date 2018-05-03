(ns memefactory.ui.home.page
  (:require
    [district.ui.component.page :refer [page]]
    [district.ui.graphql.subs :as gql]
    [memefactory.ui.home.events]
    [memefactory.ui.components.app-layout :refer [app-layout]]
    [re-frame.core :refer [subscribe]]))

(defn component2 [{:keys [:a]}]
  (let [data (subscribe [::gql/query {:queries [[:search-memes
                                                 {:a a}
                                                 [[:items [:meme/title]]]]
                                                [:meme
                                                 {:reg-entry/address "0x123"}
                                                 [:meme/title]]]}])]
    (fn []
      [app-layout
       {:meta {:title "MemeFactory"
               :description "Description"}}
       [:div
        (cond
          (:graphql/loading? @data)
          [:div "Loading..."]

          (:graphql/errors @data)
          [:div "Error: " (first (:graphql/errors @data))]

          :else
          (for [{:keys [:meme/title]} (:items (:search-memes @data))]
            [:div
             {:key title}
             title]))]])))


(defn component1 [{:keys [:id]}]
  (let [data (subscribe [::gql/query {:operation {:operation/type :query
                                                  :operation/name :memes-query}
                                      :queries [[:search-memes
                                                 {:a :$id}
                                                 [[:items [:meme/title]]]]]
                                      :variables [{:variable/name :$id
                                                   :variable/type :Int!}]}
                         {:variables {:id id}}])]
    (fn []
      [:div
       (cond
         (:graphql/loading? @data)
         [:div "Loading..."]

         (:graphql/errors @data)
         [:div "Error: " (first (:graphql/errors @data))]

         :else
         (for [{:keys [:meme/title]} (:items (:search-memes (print.foo/look @data)))]
           [:div
            {:key title}
            title]))
       [component2
        {:a 54}]])))

(defmethod page :route/home []
  [:div
   [component2
    {:a 92}]
   [component1
    {:id 77}]
   [component1
    {:id 79}]])
