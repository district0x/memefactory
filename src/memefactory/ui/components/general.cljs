(ns memefactory.ui.components.general)

(defn dank-with-logo [ammount]
  [:div.dank-wrapper
   [:span ammount]
   [:img {:src "/assets/icons/dank-logo.svg"}]])
