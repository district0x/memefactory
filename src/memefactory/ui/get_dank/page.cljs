(ns memefactory.ui.get-dank.page
  (:require
   [reagent.core :as r]
   [reagent.ratom :refer [reaction]]
   [re-frame.core :refer [subscribe dispatch]]
   [district.ui.component.page :refer [page]]
   [memefactory.ui.components.app-layout :refer [app-layout]]
   [memefactory.ui.get-dank.events :as dank-events]
   [district.format :as format]
   [district.ui.component.form.input :refer [index-by-type
                                             text-input]]))

(defn header []
  [:div.submit-info
   [:div.icon]
   [:h2.title "Receive initial DANK tokens."]
   [:h3.title "Enter your phone number and we'll send you a one-time allotment of DANK tokens."]
   [:div.get-dank-button "Get Dank"]])

(defmethod page :route.get-dank/index []
  (let [form-data (r/atom {})
        errors (reaction {:local (let [{:keys [phone-number]} @form-data]
                                   (cond-> {}
                                     (empty? phone-number)
                                     (assoc-in [:title :error] "Phone number is mandatory")))})
        critical-errors (reaction (index-by-type @errors :error))]
    (fn []
      [app-layout
       {:meta {:title "MemeFactory"
               :description "Faucet for initial DANK"}}
       [:div.dank-registry-submit-page
        [:section.submit-header
         [header]]
        [:div.form-panel
         ;; [:div (str (:local @errors))]
         ;; [:div (str @critical-errors)]
         [text-input {:form-data form-data
                      :placeholder "Phone Number"
                      :errors errors
                      :id :phone-number}]
         [:div.submit
          [:button {:on-click (fn []
                                (dispatch [::dank-events/get-initial-dank @form-data])
                                (reset! form-data {}))
                    :disabled (not (empty? @critical-errors))}
           "Submit"]]]]])))
