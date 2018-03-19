(ns memefactory.ui.graphql.effects
  (:require
    [memefactory.shared.graphql-utils :as graphql-utils]
    [re-frame.core :refer [reg-fx dispatch]]
    [venia.core :as v]))

(def print-str-graphql (aget js/GraphQL "print"))

(defn query [{:keys [:fetcher :schema :query :variables :operation-name :on-success :on-error :on-response]
              :as opts}]
  (.catch (.then (fetcher (clj->js {:query query :variables variables :operationName operation-name}))
                 (fn [res]
                   (let [res (-> (graphql-utils/transform-response res))]
                     (when (and on-success (empty? (:errors res)))
                       (dispatch (vec (concat on-success [res opts]))))
                     (when (and on-error (not (empty? (:errors res))))
                       (dispatch (vec (concat on-error [res opts]))))
                     (when on-response
                       (dispatch (vec (concat on-response [res opts])))))))
          (fn [error]
            (when on-error
              (dispatch (vec (concat on-error [error opts])))))))


(reg-fx
  ::query
  query)


(reg-fx
  ::enqueue-query
  (fn [{:keys [:fetcher :dataloader :schema :query :variables] :as opts}]
    (js-invoke dataloader "load" opts)))


(defn- create-middleware-fn [method]
  (fn [{:keys [:fetcher :middlewares :afterwares]}]
    (doseq [middleware (or middlewares afterwares)]
      (js-invoke fetcher method (fn [response options next]
                                  (when (fn? middleware)
                                    (apply middleware response options next))
                                  (when (sequential? middleware)
                                    (dispatch (vec (concat middleware [response options next])))))))))

(reg-fx
  ::add-middleware
  (create-middleware-fn "use"))

(reg-fx
  ::add-afterware
  (create-middleware-fn "useAfter"))

(reg-fx
  ::add-batch-middleware
  (create-middleware-fn "batchUse"))

(reg-fx
  ::add-batch-afterware
  (create-middleware-fn "batchUseAfter"))

(reg-fx
  ::middleware-next
  (fn [next]
    (next)))