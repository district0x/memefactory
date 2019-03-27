(ns memefactory.ui.components.general)

(defn dank-with-logo [amount]
  [:div.dank-wrapper
   [:span amount]
   [:img {:src "/assets/icons/dank-logo.svg"}]])
