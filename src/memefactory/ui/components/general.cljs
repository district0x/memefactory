(ns memefactory.ui.components.general
  (:require
    [district.ui.router.events :as router-events]
    [re-frame.core :refer [dispatch subscribe]]))


(defn dank-with-logo [amount]
  [:div.dank-wrapper
   [:span amount]
   [:img {:src "/assets/icons/dank-logo.svg"}]])


(defn nav-anchor [{:keys [route params query] :as props} & childs]
  (into [:a (merge {:on-click #(do
                                 (.preventDefault %)
                                 (when route (dispatch [::router-events/navigate route params query])))
                    :href (when route @(subscribe [:district.ui.router.subs/resolve route params query]))}
                   (dissoc props :route :params :query))]
        childs))
