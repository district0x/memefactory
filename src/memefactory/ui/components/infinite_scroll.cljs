(ns memefactory.ui.components.infinite-scroll
  (:require [reagent.core :as r]
            [taoensso.timbre :as log :refer [spy]]
            [memefactory.shared.utils :as utils]
            [react-infinite]
            ))

(def react-infinite (r/adapt-react-class js/Infinite))

(def card-height 400) ;; px
(def default-onload-threshold (* 1.5 card-height)) ;; px
(def default-debounce-interval 250) ;; ms


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

(defn- safe-component-mounted? [component]
  (try (boolean (r/dom-node component)) (catch js/Object _ false)))


(defn- get-page-height []
  (let [body (.-body js/document)
        html (.-documentElement js/document)]
    (max (.-scrollHeight body)
         (.-offsetHeight body)
         (.-clientHeight html)
         (.-scrollHeight html)
         (.-offsetHeight html))))

(defn- element-scroll-bounds [el-node]
  (let [bounds (.getBoundingClientRect el-node)]
    {:height (aget bounds "height")
     :top (aget bounds "top")
     :bottom (aget bounds "bottom")
     :page-height (get-page-height)
     :page-bottom (+ (.-innerHeight js/window) (.-scrollY js/window))}))

(defn- load-more? [el-node onload-threshold]
  (let [{:keys [page-height page-bottom]} (element-scroll-bounds el-node)
        bottom-offset (- page-height page-bottom)]
    (<= bottom-offset onload-threshold)))

(defn infinite-scroll [props]
  (let [listener-fn (atom nil)
        detach-scroll-listener (fn []
                                 (when @listener-fn
                                   (.removeEventListener js/window "scroll" @listener-fn)
                                   (.removeEventListener js/window "resize" @listener-fn)
                                   (reset! listener-fn nil)))
        should-load-more? (fn [this]
                            (let [node (r/dom-node this)
                                  ;; scroll-top (get-scroll-top)
                                  ;; my-top (get-el-top-position node)
                                  ;; threshold 50
                                  ]

                              (load-more? node default-onload-threshold)

                              ;; (log/debug "load-more?" {:load? (> threshold (- (.-offsetHeight node)
                              ;;                                                 (+ (.-innerHeight js/window)
                              ;;                                                    scroll-top)))
                              ;;                          :t threshold
                              ;;                          :v (- (.-offsetHeight node)
                              ;;                                (+ (.-innerHeight js/window)
                              ;;                                   scroll-top))})

                              ;; (> threshold
                              ;;    (- (.-offsetHeight node)
                              ;;       (+ (.-innerHeight js/window)
                              ;;          scroll-top)))

                              ))
        scroll-listener (fn [this]
                          (when (safe-component-mounted? this)
                            (let [{:keys [load-fn]} (r/props this)]
                              (when (should-load-more? this)
                                (detach-scroll-listener)
                                (load-fn)))))
        debounced-scroll-listener (utils/debounce scroll-listener default-debounce-interval)
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

#_(defn infinite-scroll [{:keys [:load-fn :element-height :infinite-load-begin-edge-offset :use-window-as-scroll-container]
                        :or {element-height 400
                             infinite-load-begin-edge-offset 100
                             use-window-as-scroll-container true}
                        :as props} & children]
  [react-infinite (r/merge-props (dissoc props :load-fn)
                                 {:element-height element-height
                                  :infinite-load-begin-edge-offset infinite-load-begin-edge-offset
                                  :use-window-as-scroll-container use-window-as-scroll-container
                                  :on-infinite-load load-fn})
   children])
