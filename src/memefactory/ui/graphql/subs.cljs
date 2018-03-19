(ns memefactory.ui.graphql.subs
  (:require
    [memefactory.ui.graphql.events :as events]
    [memefactory.ui.graphql.queries :as queries]
    [re-frame.core :refer [reg-sub-raw dispatch dispatch-sync]]
    [reagent.ratom :refer [make-reaction]]
    [venia.core :as v]))

(def gql-sync (aget js/GraphQL "graphqlSync"))

(reg-sub-raw
  ::query
  (fn [db [_ query variables]]
    (let [query (if-not (string? query) (v/graphql-query query) query)]
      (dispatch-sync [::events/query {:query query :variables variables :enqueue? true}])
      (make-reaction
        (fn []
          (queries/query @db query variables))))))

