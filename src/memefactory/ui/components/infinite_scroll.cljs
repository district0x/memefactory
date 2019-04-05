(ns memefactory.ui.components.infinite-scroll
  (:require [reagent.core :as r]
            [taoensso.timbre :as log :refer [spy]]
            [memefactory.shared.utils :as utils]))

(defn- safe-component-mounted? [component]
  (try (boolean (spy (r/dom-node component))) (catch js/Object _ false)))

(defn infinite-scroll [props]
  #_[:div "FUBAR"]
  (let [root-id "app-container"
        state (atom {:page-height 0
                     :listener-fn nil})
        update-state! #(swap! state (fn [old new] (merge old new)) %)
        get-position (fn [root-id]
                       ;; if app not mounted?
                       (let [app (-> js/document (.getElementById root-id))
                             page-height (.-offsetHeight app)
                             window-height (-> js/window .-innerHeight)
                             scroll-position (-> js/window .-scrollY)]
                         {:page-height page-height
                          :window-height window-height
                          :scroll-position scroll-position
                          :bottom? (<= page-height (+ window-height scroll-position))
                          :to-bottom (- page-height (+ window-height scroll-position))})
                       )
        should-load-more? (fn [this]
                            (let [{:keys [:infinite-load-threshold]
                                   :or {infinite-load-threshold 50}} (r/props this)
                                  {:keys [:bottom? :to-bottom] :as p} (get-position root-id)]
                              (log/debug (str "should load more? " (<= to-bottom infinite-load-threshold)) p)
                              ;; bottom?
                              (<= to-bottom infinite-load-threshold)
                              ))
        scroll-listener (fn [this]
                          ;;when (safe-component-mounted? this)
                          (let [{:keys [:load-fn :has-more? :loading?] :as p} (r/props this)]
                            (log/debug "scrolled to load more" (select-keys p [:has-more? :loading?]))
                            (when (and has-more?
                                       (not loading?)
                                       (should-load-more? this))
                              (update-state! (select-keys (get-position root-id) [:page-height]))
                              ;; (detach-scroll-listener)
                              (load-fn))))
        attach-scroll-listener (fn [this]
                                 (let [{:keys [:has-more? :debounce-interval]
                                        :or {:debounce-interval 200}} (r/props this)
                                       {:keys [:listener-fn]} @state
                                       debounced-scroll-listener (utils/debounce scroll-listener debounce-interval)]
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
     {:component-did-mount attach-scroll-listener
      :component-did-update (fn [this _]
                              (do (attach-scroll-listener this)
                                (let [{:keys [:load-fn :has-more? :loading?]} (r/props this)
                                      {:keys [:bottom? :page-height]} (get-position root-id)
                                      {previous-page-height :page-height} @state]
                                  (when (and has-more?
                                             bottom?)

                                    (log/debug "stuck at bottom, autoscrolling!" {:page-height page-height
                                                                                  :previous-page-height previous-page-height})

                                    (update-state! {:page-height page-height})
                                    (-> js/window (.scrollBy 0 (- previous-page-height page-height)))))))
      :component-will-unmount detach-scroll-listener
      :reagent-render (fn [props]
                        [:div.infinite-scroll])})))
