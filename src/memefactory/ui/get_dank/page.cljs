(ns memefactory.ui.get-dank.page
  (:require
   [clojure.string :as str]
   [district.ui.component.form.input :refer [index-by-type text-input with-label]]
   [district.ui.component.page :refer [page]]
   [memefactory.ui.components.app-layout :refer [app-layout]]
   [memefactory.ui.components.spinner :as spinner]
   [memefactory.ui.get-dank.events :as dank-events]
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as r]
   [reagent.ratom :refer [reaction]]
   [taoensso.timbre :as log]))

(def verify-through-oracle-timeout 300000)

(defmethod page :route.get-dank/index []
  (let [form-data (r/atom {})
        errors (reaction {:local (let [{:keys [country-code phone-number verification-code]} @form-data
                                       stage @(subscribe [:memefactory.ui.subs/dank-faucet-stage])]
                                   (cond-> {}
                                     (str/blank? country-code)
                                     (assoc-in [:country-code :error] "Country code is mandatory")

                                     (str/blank? phone-number)
                                     (assoc-in [:phone-number :error] "Phone number is mandatory")

                                     (and (= stage 2)
                                          (str/blank? verification-code))
                                     (assoc-in [:verification-code :error] "")))})
        critical-errors (reaction (index-by-type @errors :error))]
    (fn []
      (let [show-spinner? @(subscribe [:memefactory.ui.subs/dank-faucet-spinner])]
        [app-layout
         {:meta {:title "MemeFactory - Receive initial DANK tokens"
                 :description "Enter your phone number and we'll send you a one-time allotment of DANK tokens. MemeFactory is decentralized registry and marketplace for the creation, exchange, and collection of provably rare digital assets."}}
         [:div.get-dank-page
          [:div.get-dank-box
           [:div.icon]
           [:h2.title "Receive initial DANK tokens"]
           [:h3.title "Enter your phone number and we'll send you a one-time allotment of DANK tokens"]
           [:div.body
            (if show-spinner?
              [spinner/spin]
              (let [stage @(subscribe [:memefactory.ui.subs/dank-faucet-stage])]
                (log/debug "Faucet stage:" stage)
                (case stage
                  1 [:div.form

                     [with-label
                      "Country Code"
                      [text-input (merge {:form-data form-data
                                          :key :country-code
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
                                          :key :phone-number
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
                                          :key :verification-code
                                          :dom-id :verification-code
                                          :id :verification-code})]
                      {:form-data form-data
                       :for :verification-code
                       :id :verification-code}]])))]
           (when-not show-spinner?
             [:button.footer
              {:on-click (fn []
                           (let [verification-code (:verification-code @form-data)]
                             (if (clojure.string/blank? verification-code)
                               (do ; Stage 1
                                 (dispatch [::dank-events/get-allocated-dank @form-data])
                                 (js/setTimeout #(dispatch [::dank-events/hide-spinner])
                                                verify-through-oracle-timeout))
                               (do ; Stage 2
                                 (dispatch [::dank-events/encrypt-verification-payload @form-data])
                                 (reset! form-data {})))))
               :disabled (not (empty? @critical-errors))}
              "Submit"])]]]))))
