(ns memefactory.ui.components.infinite-scroll
  (:require
   [goog.functions]
   [reagent.core :as r]
   [taoensso.timbre :as log]))


(def card-height 290) ;; px
(def debounce goog.functions.debounce)
(def default-debounce-interval 250) ;; ms
(def default-onload-threshold (* 1.5 card-height)) ;; px


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


(defn- should-load-more? [el-node onload-threshold]
  (let [{:keys [page-height page-bottom]} (element-scroll-bounds el-node)
        bottom-offset (- page-height page-bottom)]
    (<= bottom-offset onload-threshold)))


(defn- create-scroll-listener [el-node onload-callback]
  (debounce
   (fn [e]
     (when (should-load-more? el-node default-onload-threshold)
       (log/debug "Infinite Scroll Triggered")
       (if onload-callback
         (onload-callback)
         (throw (ex-info "Onload Callback not set for infinite scroll" {}))))
     true)
   default-debounce-interval))


(defn infinite-scroll [{:keys [load-fn] :as props}]
  (let [listener-store (atom nil)]
    (r/create-class
     {:component-did-mount
      (fn [this]
        (let [parent-element (-> (r/dom-node this) .-parentElement)
              scroll-listener (create-scroll-listener parent-element load-fn)]
          (reset! listener-store (partial scroll-listener this))
          (.addEventListener js/window "scroll" @listener-store)
          (.addEventListener js/window "resize" @listener-store)))

      :component-will-unmount
      (fn [_]
        (.removeEventListener js/window "scroll" @listener-store)
        (.removeEventListener js/window "resize" @listener-store))

      :reagent-render
      (fn []
        [:div.infinite-scroll])})))
