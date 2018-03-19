(ns memefactory.ui.graphql.utils
  (:require
    [cljsjs.dataloader]
    [memefactory.shared.graphql-utils :as graphql-utils]
    [re-frame.core :refer [dispatch]]))

(def print-str-graphql (aget js/GraphQL "print"))

(defn create-dataloader [{:keys [:on-success :on-error :transform-name-fn]}]
  (let [dt (atom nil)]
    (reset! dt (js/DataLoader.
                 (fn [queries]
                   (let [queries (vec queries)
                         query-ast (graphql-utils/merge-queries (map :query-ast queries))
                         query (print-str-graphql query-ast)
                         variables (apply merge (map :variables queries))
                         {:keys [:fetcher]} (first queries)]
                     (print.foo/look query)
                     (js-invoke @dt "clearAll")
                     (let [res-promise (js/Promise.
                                         (fn [resolve reject]
                                           (.catch (.then (fetcher (clj->js {:query query :variables variables}))
                                                          (fn [res]
                                                            (let [res (graphql-utils/transform-response res)]
                                                              (if (empty? (:errors res))
                                                                (do
                                                                  (when on-success
                                                                    (let [res-opts
                                                                          (merge (first queries)
                                                                                 {:query query
                                                                                  :query-ast query-ast
                                                                                  :variables variables})]
                                                                      (dispatch (vec (concat on-success [(:data res) res-opts])))))
                                                                  (resolve (.fill (js/Array. (count queries)) res)))
                                                                (do
                                                                  (when on-error
                                                                    (dispatch (vec (concat on-error [res queries]))))
                                                                  (reject (:errors res)))))))
                                                   (fn [error]
                                                     (reject error)))))]
                       res-promise)))))))