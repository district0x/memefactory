(ns memefactory.ui.components.general
  (:require
    [district.ui.router.events :as router-events]
    [re-frame.core :refer [dispatch subscribe]]
    [memefactory.ui.subs :as mf-subs]
    [memefactory.ui.events :as mf-events]
    [reagent.core :as r]
    [district.ui.component.form.input :as input]))


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

(defn nsfw-switch [form-data]
  (let [id :nsfw-switch]
    [:div.nsfw-switch.single-check
     [input/checkbox-input {:form-data form-data
                            :id id
                            :on-change #(dispatch [::mf-events/nsfw-switch]) }]
     [:label {:on-click #(swap! form-data update id not)}
      "NSFW"]]))
