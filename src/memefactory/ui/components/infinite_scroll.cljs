(ns memefactory.ui.components.infinite-scroll
  (:require [reagent.core :as r]
            [taoensso.timbre :as log :refer [spy]]
            [memefactory.shared.utils :as utils]
            [react-infinite]))

(def react-infinite (r/adapt-react-class js/Infinite))

(defn- safe-component-mounted? [component]
  (try (boolean (spy (r/dom-node component))) (catch js/Object _ false)))

#_(defn infinite-scroll [props]
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
                            :to-bottom (- page-height (+ window-height scroll-position))}))
          should-load-more? (fn [this]
                              (let [{:keys [:infinite-load-threshold]
                                     :or {infinite-load-threshold 50}} (r/props this)
                                    {:keys [:bottom? :to-bottom]} (get-position root-id)]
                                (<= to-bottom infinite-load-threshold)))
          scroll-listener (fn [this evt]

                            #_(log/debug "rect" {:r (get-client-rect (r/dom-node this)
                                                                     (-> js/document (.getElementById root-id))
                                                                     evt)} )

                            (let [{:keys [:load-fn :has-more? :loading?]} (r/props this)
                                  load-more? (and has-more?
                                                  (not loading?)
                                                  (should-load-more? this))]

                              (log/debug "scroll event" (select-keys (merge {:load-more? load-more?}
                                                                            (get-position root-id))
                                                                     [:page-height
                                                                      :previous-page-height
                                                                      :to-bottom
                                                                      :scroll-position
                                                                      :window-height]))

                              (when load-more?
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
                                       (update-state! {:listener-fn nil}))))]
      (r/create-class
       {:component-did-mount attach-scroll-listener
        :component-did-update (fn [this _]
                                (do (attach-scroll-listener this)
                                    (let [{:keys [:load-fn :has-more? :loading?]} (r/props this)
                                          {:keys [:bottom? :page-height] :as pos} (get-position root-id)
                                          {previous-page-height :page-height} @state]

                                      (when (and has-more?
                                                 bottom?)

                                        (log/debug "stuck at bottom, autoscrolling!" (select-keys (merge {:previous-page-height previous-page-height}
                                                                                                         pos)
                                                                                                  [:page-height
                                                                                                   :previous-page-height
                                                                                                   :to-bottom
                                                                                                   :scroll-position
                                                                                                   :window-height]))



                                        (-> js/window (.scrollBy 0 (spy (- previous-page-height page-height))))
                                        (update-state! {:page-height page-height})

                                        ))))
        :component-will-unmount detach-scroll-listener
        :reagent-render (fn [props]
                          [:div.infinite-scroll])})))

(defn- get-page-height [root-id]
  (.-offsetHeight (-> js/document (.getElementById root-id))))

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

(defn infinite-scroll [{:keys [:class :element-height :use-window-as-scroll-container
                               :container-height :infinite-load-threshold
                               :loading?
                               :has-more?
                               :loading-spinner-delegate
                               :load-fn]
                        :or {class :infinite-scroll
                             use-window-as-scroll-container true
                             element-height 435
                             ;; container-height (get-page-height "app-container")
                             infinite-load-threshold 50
                             loading-spinner-delegate (fn []
                                                        [:div.loading-spinner-delegate {:style {:height "435px"
                                                                                                :text-align :center
                                                                                                :width "290px"
                                                                                                :margin :auto
                                                                                                :background-color "#ddd"
                                                                                                :border-bottom "1px solid #000"
                                                                                                :border-radius "1em"
                                                                                                :white-space :nowrap
                                                                                                :overflow :hidden
                                                                                                :text-overflow :ellipsis}}
                                                         "Loading..." ])}
                        :as props} & [children]]
  (into [react-infinite (r/merge-props (dissoc props :load-fn :loading?)
                                       {:class #_"tiles" (name class)
                                        :element-height element-height
                                        :infinite-load-begin-edge-offset element-height ;;infinite-load-threshold
                                        :use-window-as-scroll-container use-window-as-scroll-container
                                        :container-height (-> js/window .-innerHeight) ;;container-height
                                        :loading-spinner-delegate loading-spinner-delegate
                                        :is-infinite-loading loading?
                                        :handle-scroll (fn [evt]
                                                         (let [{:keys [:to-bottom :page-height] :as pos} (get-position "app-container")]
                                                           (if (and  has-more? 
                                                                     (<= to-bottom 10 #_infinite-load-threshold))
                                                             (do (log/debug "stuck at bottom, autoscrolling!")
                                                                 (-> js/window (.scrollBy 0 -500))))

                                                           ))
                                        :time-scroll-state-lasts-for-after-user-scrolls 1000
                                        :on-infinite-load (fn []
                                                            (when (not loading?)
                                                              (load-fn)))})
         ] children))
