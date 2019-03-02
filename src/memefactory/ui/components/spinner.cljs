(ns memefactory.ui.components.spinner)

(defn spin []
  [:div.spinner-outer
   [:img {:src "/assets/icons/mememouth.png"}]
   [:svg.spinner-inner {:viewBox"0 0 66 66"}
    [:circle {:fill "#FFFF"
              :cx "33"
              :cy "33"
              :r "30"
              :stroke-width "2"
              :stroke "url(#gradient)"
              :class "spinner-path"}]
    [:linearGradient {:id "gradient"}
     [:stop {:offset "50%"
             :stop-color "#e7ebf8"
             :stop-opacity "1"}]
     [:stop {:offset "65%"
             :stop-color "#e7ebf8"
             :stop-opacity ".5"}]
     [:stop {:offset "100%"
             :stop-color "#e7ebf8"
             :stop-opacity "0"}]]]])
