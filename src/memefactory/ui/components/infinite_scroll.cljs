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
  (let [state (atom {:page-height 0
                     :listener-fn nil})
        update-state! #(swap! state (fn [old new] (merge old new)) %)
        get-position (fn [root-id] (let [page-height (.-offsetHeight (-> js/document (.getElementById root-id)))
                                         window-height (-> js/window .-innerHeight)
                                         scroll-position (-> js/window .-scrollY)]
                                     {:page-height page-height
                                      :window-height window-height
                                      :scroll-position scroll-position
                                      :bottom? (<= page-height (+ window-height scroll-position))}))
        should-load-more? (fn [this]
                            (:bottom? (get-position "app-container")))
        scroll-listener (fn [this]
                          (when (safe-component-mounted? this)
                            (let [{:keys [:load-fn :has-more? :loading?]} (r/props this)]
                              (when (and has-more?
                                         (not loading?)
                                         (should-load-more? this))

                                (update-state! (select-keys (get-position "app-container") [:page-height]))
                                ;; (detach-scroll-listener)
                                (load-fn)

                                ))))
        debounced-scroll-listener (utils/debounce scroll-listener 200)
        attach-scroll-listener (fn [this]
                                 (let [{:keys [:has-more?]} (r/props this)
                                       {:keys [:listener-fn]} @state]
                                   (when has-more?
                                     (when-not listener-fn
                                       (update-state! {:listener-fn (partial debounced-scroll-listener this)})
                                       (.addEventListener js/window "scroll" (:listener-fn @state))
                                       (.addEventListener js/window "resize" (:listener-fn @state))))))
        detach-scroll-listener (fn []
                                 (let [{:keys [:listener-fn]} @state]
                                   (when listener-fn
                                     (.removeEventListener js/window "scroll" listener-fn)
                                     (.removeEventListener js/window "resize" listener-fn)
                                     (reset! listener-fn nil))))]
    (r/create-class
     {:component-did-mount
      (fn [this]
        (attach-scroll-listener this))
      :component-did-update
      (fn [this _]
        (attach-scroll-listener this)
        (let [{:keys [:load-fn :has-more? :loading?]} (r/props this)
              {:keys [:bottom? :page-height]} (get-position "app-container")
              {previous-page-height :page-height} @state]
          (when (and has-more?
                     bottom?)

            (log/debug "stuck at bottom, autoscrolling!" {:page-height page-height
                                                          :previous-page-height previous-page-height})

            (update-state! {:page-height page-height})
            (-> js/window (.scrollBy 0 (- previous-page-height page-height)))


            )


          ))
      :component-will-unmount
      detach-scroll-listener
      :reagent-render
      (fn [props]
        [:div])})))
