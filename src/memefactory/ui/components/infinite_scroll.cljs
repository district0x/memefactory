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

(defn- get-scroll-top []
  (if (exists? (.-pageYOffset js/window))
    (.-pageYOffset js/window)
    (.-scrollTop (or (.-documentElement js/document)
                     (.-parentNode (.-body js/document))
                     (.-body js/document)))))

(defn- get-el-top-position [node]
  (if (not node)
    0
    (+ (.-offsetTop node) (get-el-top-position (.-offsetParent node)))))

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

                            (let [app (-> js/document (.getElementById "app-container"))
                                  page-height (-> app .-offsetHeight)
                                  window-height (-> js/window .-innerHeight)
                                  scroll-position (-> js/window .-scrollY)
                                  bottom? (<= page-height (+ window-height scroll-position))]

                              (log/info "@should-load-more?" {:bottom? bottom?})

                              bottom?

                              )

                            #_(let [node (r/dom-node this)
                                  scroll-top (get-scroll-top)
                                  my-top (get-el-top-position node)
                                  threshold 250]
                              (< (- (+ my-top (.-offsetHeight node))
                                    scroll-top
                                    (.-innerHeight js/window))
                                 threshold)))
        scroll-listener (fn [this]
                          (when (safe-component-mounted? this)
                            (let [{:keys [:load-fn :has-more? :loading?]} (r/props this)]
                              (when (and has-more?
                                         (not loading?)
                                         (should-load-more? this))

                                (log/debug "loading more...")

                                (detach-scroll-listener)
                                (load-fn)

                                ))))
        debounced-scroll-listener (utils/debounce scroll-listener 200)
        attach-scroll-listener (fn [this]
                                 (let [{:keys [:has-more?]} (r/props this)]
                                   (when has-more?
                                     (when-not @listener-fn
                                       (reset! listener-fn (partial debounced-scroll-listener this))
                                       (.addEventListener js/window "scroll" @listener-fn)
                                       (.addEventListener js/window "resize" @listener-fn)))))]
    (r/create-class
      {:component-did-mount
       (fn [this]
         (attach-scroll-listener this))
       :component-did-update
       (fn [this _]
         (attach-scroll-listener this))
       :component-will-unmount
       detach-scroll-listener
       :reagent-render
       (fn [props]
         [:div])})))
