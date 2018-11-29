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
                                             text-input]]
   [district.ui.graphql.subs :as gql]))

(defn header []
  [:div.submit-info
   [:div.icon]
   [:h2.title "Receive initial DANK tokens."]
   [:h3.title "Enter your phone number and we'll send you a one-time allotment of DANK tokens."]
   [:div.get-dank-button "Get Dank"]])

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
      [app-layout
       {:meta {:title "MemeFactory"
               :description "Faucet for initial DANK"}}
       [:div.dank-registry-submit-page
        [:section.submit-header
         [header]]
        [:div.form-panel
         [text-input (merge {:form-data form-data
                             :placeholder "Country Code"
                             :errors errors
                             :id :country-code}
                            (when (= @stage 2)
                              {:disabled "disabled"}))]
         [text-input (merge {:form-data form-data
                             :placeholder "Phone Number"
                             :errors errors
                             :id :phone-number}
                            (when (= @stage 2)
                              {:disabled "disabled"}))]
         [text-input (merge {:form-data form-data
                             :placeholder "Verification Code"
                             :errors errors
                             :id :verification-code}
                            (when (= @stage 1)
                              {:style
                               {:visibility "hidden"}}))]
         [:div.submit
          [:button {:on-click (fn []
                                (let [verification-code (:verification-code @form-data)]
                                  (println "verification-code blank?:"
                                           (clojure.string/blank? verification-code))
                                  (if (clojure.string/blank? verification-code)
                                    (do ; Stage 1
                                      (dispatch [::dank-events/send-verification-code @form-data])
                                      (reset! stage 2))
                                    (do ; Stage 2
                                      (dispatch [::dank-events/encrypt-verification-payload @form-data])
                                      (reset! form-data {})
                                      (reset! stage 1)))))
                    :disabled (not (empty? @critical-errors))}
           "Submit"]]]]])))
