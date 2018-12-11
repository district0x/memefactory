(ns memefactory.ui.components.charts
  (:require [cljsjs.d3]
            [reagent.core :as r]))

(defn donut-chart [{:keys [:reg-entry/address :challenge/votes-for :challenge/votes-against :challenge/votes-total]}]
  (r/create-class
   {:reagent-render (fn [{:keys [:reg-entry/address :challenge/votes-for :challenge/votes-against :challenge/votes-total]}]
                      [:div {:id (str "donutchart-" address)}])
    :component-did-mount (fn []
                           (let [width 150
                                 height 150
                                 icon-width 42
                                 icon-height 32
                                 data [{:challenge/votes :for :value votes-for}
                                       {:challenge/votes :against :value votes-against}]
                                 outer-radius (/ (min width height) 2)
                                 inner-radius (/ outer-radius 2)
                                 arc (-> js/d3
                                         .arc
                                         (.outerRadius outer-radius)
                                         (.innerRadius inner-radius))
                                 pie (-> js/d3
                                         .pie
                                         (.value (fn [d] (aget d "value"))))
                                 color-scale (-> js/d3
                                                 .scaleOrdinal
                                                 (.range (clj->js ["#04ffcc" "#ffeb01"])))]
                             (-> js/d3
                                 (.select (str "#donutchart-" address))
                                 (.append "svg")
                                 (.attr "class" (str "chart-" address))
                                 (.attr "width" width)
                                 (.attr "height" height)
                                 (.append "g")
                                 (.attr "transform" (str "translate(" (/ width 2) "," (/ height 2) ")"))
                                 (.selectAll ".arc")
                                 (.data (pie (clj->js data)))
                                 (.enter)
                                 (.append "g")
                                 (.attr "class" "arc")
                                 (.append "path")
                                 (.attr "d" arc)
                                 (.style "fill" (fn [d]
                                                  (color-scale
                                                   (aget d "data" "votes")))))

                             (-> js/d3
                                 (.select (str ".chart-" address))
                                 (.append "g")
                                 (.attr "transform" (str "translate(" (/ width 2) "," (/ height 2) ")"))
                                 (.append "foreignObject")
                                 (.attr "width" icon-width)
                                 (.attr "height" icon-height)
                                 (.attr "x" (unchecked-negate (/ icon-width 2)))
                                 (.attr "y" (unchecked-negate (/ icon-height 2)))
                                 (.append "xhtml:span")
                                 (.append "span")
                                 (.attr "class" "icon-mf-logo")
                                 (.style "font-size" "32px")
                                 (#(doall (for [i (range 1 9)]
                                            (-> (.append % "span")
                                                (.attr "class" (str "path" i)))))))))}))
