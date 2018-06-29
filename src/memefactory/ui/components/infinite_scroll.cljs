(ns memefactory.ui.components.infinite-scroll
  (:require [reagent.core :as r]))

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

(defn debounce
  ([threshold f] (debounce threshold f (constantly nil)))
  ([threshold f prep-fn]
   (let [t (atom nil)]
     (fn [& args]
       (when @t (js/clearTimeout @t))
       (apply prep-fn args)
       (reset! t (js/setTimeout #(do
                                   (reset! t nil)
                                   (apply f args))
                                threshold))))))

(defn infinite-scroll [props]
  (let [listener-fn (atom nil)
        detach-scroll-listener (fn []
                                 (when @listener-fn
                                   (.removeEventListener js/window "scroll" @listener-fn)
                                   (.removeEventListener js/window "resize" @listener-fn)
                                   (reset! listener-fn nil)))
        should-load-more? (fn [this]
                            (let [node (r/dom-node this)
                                  scroll-top (get-scroll-top)
                                  my-top (get-el-top-position node)
                                  threshold 50]
                              (< (- (+ my-top (.-offsetHeight node))
                                    scroll-top
                                    (.-innerHeight js/window))
                                 threshold)))
        scroll-listener (fn [this]
                          (when (safe-component-mounted? this)
                            (let [{:keys [load-fn]} (r/props this)]
                              (when (should-load-more? this)
                                (detach-scroll-listener)
                                (load-fn)))))
        debounced-scroll-listener (debounce 200 scroll-listener)
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
