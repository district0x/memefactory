(ns memefactory.ui.get-dank.page
  (:require
   [reagent.core :as r]
   [reagent.ratom :refer [reaction]]
   [re-frame.core :refer [subscribe dispatch]]
   [district.ui.component.page :refer [page]]
   [memefactory.ui.components.app-layout :refer [app-layout]]
   [memefactory.ui.get-dank.events :as dank-events]
   [memefactory.ui.components.spinner :as spinner]
   [district.format :as format]
   [district.ui.component.form.input :refer [index-by-type
                                             text-input
                                             with-label]]
   [district.ui.graphql.subs :as gql]))

(def verify-through-oracle-timeout 300000)

(defmethod page :route.get-dank/index []
  (let [form-data (r/atom {})
        stage (r/atom 1)
        errors (reaction {:local (let [{:keys [country-code phone-number]} @form-data]
                                   (cond-> {}
                                     (empty? country-code)
                                     (assoc-in [:country-code :error] "Country code is mandatory")

                                     (empty? phone-number)
                                     (assoc-in [:phone-number :error] "Phone number is mandatory")))})
        critical-errors (reaction (index-by-type @errors :error))]
    (fn []
      (let [show-spinner? @(subscribe [:memefactory.ui.subs/dank-faucet-spinner])]
        [app-layout
         {:meta {:title "MemeFactory"
                 :description "Faucet for initial DANK"}}
         [:div.get-dank-page
          [:div.get-dank-box
           [:div.icon]
           [:h2.title "Receive initial DANK tokens"]
           [:h3.title "Enter your phone number and we'll send you a one-time allotment of DANK tokens"]
           [:div.body
            (if show-spinner?
              [spinner/spin]
              (case @stage
                1 [:div.form

                   [with-label
                    "Country Code"
                    [text-input (merge {:form-data form-data
                                        :errors errors
                                        :id :country-code
                                        :dom-id :country-code
                                        :class "country-code"})]
                    {:form-data form-data
                     :for :country-code
                     :id :country-code}]
                   [with-label
                    "Phone Number"
                    [text-input (merge {:form-data form-data
                                        :errors errors
                                        :id :phone-number
                                        :dom-id :phone-number
                                        :class "phone"})]
                    {:form-data form-data
                     :for :phone-number
                     :id :phone-number}]]
                2 [:div.form
                   [with-label
                    "Verification Code"
                    [text-input (merge {:form-data form-data
                                        :errors errors
                                        :dom-id :verification-code
                                        :id :verification-code})]
                    {:form-data form-data
                     :for :verification-code
                     :id :verification-code}]]))]
           (when-not show-spinner?
             [:div.footer
              {:on-click (fn []
                           (let [verification-code (:verification-code @form-data)]
                             (println "verification-code blank?:"
                                      (clojure.string/blank? verification-code))
                             (if (clojure.string/blank? verification-code)
                               (do ; Stage 1
                                 (dispatch [::dank-events/send-verification-code @form-data])
                                 (js/setTimeout #(dispatch [::dank-events/hide-spinner])
                                                verify-through-oracle-timeout)
                                 (reset! stage 2))
                               (do ; Stage 2
                                 (dispatch [::dank-events/encrypt-verification-payload @form-data])
                                 (reset! form-data {})
                                 (reset! stage 1)))))
               :disabled (not (empty? @critical-errors))}
              "Submit"])]]]))))
