(ns memefactory.ui.home.page
  (:require
    [district.ui.component.page :refer [page]]
    [memefactory.ui.graphql.subs :as gql]
    [memefactory.ui.home.events]
    [re-frame.core :refer [subscribe]]
    ))

(defn component2 []
  (let [data (subscribe [::gql/query
                         {:venia/queries [{:query/data [:search-memes
                                                        {:a 2}
                                                        [[:items [:meme/title]]]]
                                           :query/alias :loool}
                                          [:meme
                                           {:reg-entry/address "0x123"}
                                           [:meme/title]]]}])]
    (fn []
      [:div
       (for [{:keys [:meme/title]} (:items (:search-memes (print.foo/look @data)))]
         [:div
          {:key title}
          title])])))


(defn component1 []
  (let [data (subscribe
               #_[::gql/query
                  {:venia/operation {:operation/type :query
                                     :operation/name "memesQuery"}
                   :venia/queries [[:search-memes
                                    {:a 87}
                                    [[:items [:meme/title]]]]]}]

               [::gql/query
                {:venia/operation {:operation/type :query
                                   :operation/name "memesQuery"}
                 :venia/queries [[:search-memes
                                  {:a :$id}
                                  [[:items [:meme/title]]]]]
                 :venia/variables [{:variable/name "id"
                                    :variable/type :Int!}]}
                {:id 99}])]
    (fn []
      [:div
       (for [{:keys [:meme/title]} (:items (:search-memes (print.foo/look @data)))]
         [:div
          {:key title}
          title])])))

(defmethod page :route/home []
  [:div
   [component2]
   [component1]])