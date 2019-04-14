(ns memefactory.ui.components.infinite-scroll
  (:require [reagent.core :as r]
            [taoensso.timbre :as log :refer [spy]]
            [memefactory.shared.utils :as utils]
            [re-frame.core :refer [subscribe]]
            [district.ui.window-size.subs :as w-size-subs]
            [react-infinite]))

(def react-infinite (r/adapt-react-class js/Infinite))

(defn get-position [root-id]
  (let [app (-> js/document (.getElementById root-id))
        page-height (.-offsetHeight app)
        window-height (-> js/window .-innerHeight)
        scroll-position (-> js/window .-scrollY)]
    {:page-height page-height
     :window-height window-height
     :scroll-position scroll-position
     :bottom? (<= page-height (+ window-height scroll-position))
     :to-bottom (- page-height (+ window-height scroll-position))}))

(defn preload-batch-size [factor]
  (js-invoke js/Infinite "containerHeightScaleFactor" factor))

(defn infinite-scroll [{:keys [:class
                               :use-window-as-scroll-container
                               :element-height
                               :container-height
                               :elements-in-row
                               :loading-spinner-delegate
                               :load-fn
                               :loading?
                               :has-more?]
                        :or {class "infinite-scroll"
                             use-window-as-scroll-container true
                             element-height 435
                             elements-in-row 3
                             container-height (-> js/window .-innerHeight)
                             loading-spinner-delegate (fn []
                                                        [:div.loading-spinner-delegate
                                                         "Loading..." ])}
                        :as props} & [children]]

  (let [tutorial-next-fired? (atom false)]
    (fn [props & [children]]
      (let [row-height (if @(subscribe [::w-size-subs/mobile?])
                         element-height
                         (quot element-height elements-in-row))]

        (when (and (not @tutorial-next-fired?)
                   (.-enjoy-hint-tutorial js/window)
                   (pos? (count children)))
          (.trigger (.-enjoy-hint-tutorial js/window) "next")
          (reset! tutorial-next-fired? true))

        (into [react-infinite (r/merge-props (dissoc props :load-fn :loading?)
                                             {:class class
                                              :element-height row-height
                                              :infinite-load-begin-edge-offset row-height
                                              :use-window-as-scroll-container use-window-as-scroll-container
                                              :container-height container-height
                                              :loading-spinner-delegate loading-spinner-delegate
                                              :is-infinite-loading loading?
                                              :handle-scroll (fn [evt]
                                                               (let [{:keys [:to-bottom :page-height] :as pos} (get-position "app-container")]
                                                                 (when (and has-more?
                                                                            (<= to-bottom 10))
                                                                   (log/debug "stuck at bottom, autoscrolling!" (merge props
                                                                                                                       pos))
                                                                   (-> js/window (.scrollBy 0 (- 0 element-height))))))
                                              :time-scroll-state-lasts-for-after-user-scrolls 1000
                                              :preload-batch-size (preload-batch-size 2)
                                              :on-infinite-load (fn []
                                                                  (when (and has-more?
                                                                             (not loading?))
                                                                    (log/debug "loading more")
                                                                    (load-fn)))})]
              children)))))
