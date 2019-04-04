(ns memefactory.ui.components.infinite-scroll
  (:require [reagent.core :as r]
            [taoensso.timbre :as log :refer [spy]]
            [memefactory.shared.utils :as utils]
            [react-infinite]
            ))

(def react-infinite (r/adapt-react-class js/Infinite))

(def default-debounce-interval 200) ;; ms

(defn- safe-component-mounted? [component]
  (try (boolean (r/dom-node component)) (catch js/Object _ false)))

(defn get-page-height []
  (try (-> (-> js/document (.getElementById "app-container") ) .-offsetHeight)
       (catch :default e
         0)))

#_(defn infinite-scroll [{:keys [:class :element-height :use-window-as-scroll-container
                               :container-height :infinite-load-begin-edge-offset
                               :loading?
                               :loading-spinner-delegate
                               :load-fn]
                        :or {class :infinite-scroll
                             use-window-as-scroll-container true
                             element-height 400
                             container-height (get-page-height)
                             infinite-load-begin-edge-offset 200
                             loading-spinner-delegate (fn []
                                                        [:div.loading-spinner-delegate "Loading..." ])}
                        :as props} & [children]]
  [react-infinite (r/merge-props (dissoc props :load-fn :loading?)
                                 {:class (name class)
                                  :element-height element-height
                                  :use-window-as-scroll-container use-window-as-scroll-container
                                  :container-height container-height
                                  :loading-spinner-delegate loading-spinner-delegate
                                  :is-infinite-loading loading?
                                  :on-infinite-load load-fn
                                  #_(fn []
                                      (when (not loading?)
                                        (load-fn)))})
   children])

(defn infinite-scroll [props]
  (let [listener-fn (atom nil)
        detach-scroll-listener (fn []
                                 (when @listener-fn
                                   (.removeEventListener js/window "scroll" @listener-fn)
                                   (.removeEventListener js/window "resize" @listener-fn)
                                   (reset! listener-fn nil)))
        should-load-more? (fn [this]
                            (let [node (r/dom-node this)
                                  app (-> js/document (.getElementById "app-container"))
                                  page-height (-> app .-offsetHeight)
                                  window-height (-> js/window .-innerHeight)
                                  scroll-position (-> js/window .-scrollY)
                                  to-page-bottom (- page-height (+ window-height scroll-position))]

                              ;; (load-more? node default-onload-threshold)

                              (log/info "load-more?" {:page-height page-height
                                                      :window-height window-height
                                                      :scroll-position scroll-position
                                                      :to-bottom to-page-bottom
                                                      :bottom? (<= page-height (+ window-height scroll-position))})

                              ;; (<= page-height (+ window-height scroll-position))

                              ;; true
                              (< to-page-bottom 150)

                              ))
        scroll-listener (fn [this]

                          (log/info "@listener")

                          (when (safe-component-mounted? this)
                            (let [{:keys [:load-fn]} (r/props this)]
                              (when (should-load-more? this)
                                #_(detach-scroll-listener)
                                (load-fn)))))
        debounced-scroll-listener (utils/debounce scroll-listener 200)
        attach-scroll-listener (fn [this]
                                 (when-not @listener-fn
                                   (reset! listener-fn (partial debounced-scroll-listener this))
                                   (.addEventListener js/window "scroll" @listener-fn)
                                   (.addEventListener js/window "resize" @listener-fn)))]
    (r/create-class
     {:component-did-mount attach-scroll-listener
      :component-did-update attach-scroll-listener
      :component-will-unmount detach-scroll-listener
      :reagent-render (fn [] [:div.infinite-scroll])})))
