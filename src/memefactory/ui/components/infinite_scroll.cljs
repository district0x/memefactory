(ns memefactory.ui.components.infinite-scroll
  (:require [reagent.core :as r]
            [taoensso.timbre :as log :refer [spy]]
            [memefactory.shared.utils :as utils]
            [react-infinite]
            ))

(def react-infinite (r/adapt-react-class js/Infinite))

(defn- get-page-height []
  (let [body (.-body js/document)
        html (.-documentElement js/document)]
    (max (.-scrollHeight body)
         (.-offsetHeight body)
         (.-clientHeight html)
         (.-scrollHeight html)
         (.-offsetHeight html))))

(defn infinite-scroll [{:keys [:class :element-height :use-window-as-scroll-container
                               :container-height :infinite-load-begin-edge-offset
                               :loading?
                               :loading-spinner-delegate
                               :load-fn]
                        :or {class :infinite-scroll
                             element-height 400
                             use-window-as-scroll-container true
                             container-height (get-page-height)
                             loading-spinner-delegate (fn []
                                                        [:div.loading-spinner-delegate "Loading..." ])}
                        :as props} & [children]]
  [react-infinite (r/merge-props (dissoc props :load-fn :is-infinite-loading :infinite-load-begin-edge-offset)
                                 {:class class
                                  :element-height element-height
                                  :use-window-as-scroll-container use-window-as-scroll-container
                                  :container-height container-height
                                  :infinite-load-begin-edge-offset (* 3 element-height)
                                  :loading-spinner-delegate loading-spinner-delegate
                                  :is-infinite-loading loading?
                                  :on-infinite-load (fn []
                                                      (when (not loading?)
                                                        (load-fn)))})
   children])


#_(defn infinite-scroll [props & [children]]
    (let [load-disabled? (r/atom false)]
      (fn [props & [children]]
        (let [{:keys [:class :element-height :use-window-as-scroll-container
                      :container-height :infinite-load-begin-edge-offset
                      :loading?
                      :loading-spinner-delegate
                      :load-fn]
               :or {class :infinite-scroll
                    element-height 400
                    use-window-as-scroll-container true
                    container-height (get-page-height)
                    loading-spinner-delegate (fn []
                                               [:div.loading-spinner-delegate "Loading..." ])}
               } props]
          [react-infinite (r/merge-props (dissoc props :load-fn :is-infinite-loading :infinite-load-begin-edge-offset)
                                         {:class class
                                          :element-height element-height
                                          :use-window-as-scroll-container use-window-as-scroll-container
                                          :container-height container-height
                                          :infinite-load-begin-edge-offset (* 3 element-height)
                                          :loading-spinner-delegate loading-spinner-delegate
                                          :is-infinite-loading loading?
                                          :on-infinite-load (fn []
                                                              (when (and #_(not (spy @load-disabled?))
                                                                         (not loading?))
                                                                (reset! load-disabled? true)
                                                                (js/setTimeout #(reset! load-disabled? false) 300)
                                                                (load-fn)
                                                                (reset! load-disabled? false)))})
           children]))))
