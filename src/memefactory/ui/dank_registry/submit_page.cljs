(ns memefactory.ui.dank-registry.submit-page
  (:require
   [cljs-web3.core :as web3]
   [clojure.string :as str]
   [district.parsers :as parsers]
   [district.ui.component.form.input :refer [index-by-type file-drag-input with-label chip-input text-input int-input]]
   [district.ui.component.page :refer [page]]
   [district.ui.graphql.subs :as gql]
   [district.ui.web3-account-balances.subs :as balance-subs]
   [district.ui.web3-accounts.subs :as accounts-subs]
   [district.ui.web3-tx-id.subs :as tx-id-subs]
   [goog.string :as gstring]
   [memefactory.ui.components.app-layout :refer [app-layout]]
   [memefactory.ui.components.buttons :refer [chain-check-pending-button]]
   [memefactory.ui.components.general :refer [nav-anchor dank-with-logo]]
   [memefactory.ui.components.tiles :refer [meme-image]]
   [memefactory.ui.contract.meme-factory :as meme-factory]
   [memefactory.ui.dank-registry.events :as dr-events]
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as r]
   [reagent.ratom :refer [reaction]]
   [taoensso.timbre :as log :refer [spy]]))

(defn header []
  (let [active-account (subscribe [::accounts-subs/active-account])]
    (fn []
      (let [account-active? (boolean @active-account)]
        [:div.submit-info
         [:div.icon]
         [:h2.title "Dank registry - Submit"]
         [:h3.title "Submit a new meme to the registry for consideration"]
         [nav-anchor {:route (when account-active? :route.get-dank/index)}
          [:div.get-dank-button
           {:class (when-not account-active? "disabled")}
           [:span "Get Dank"]
           [:img.dank-logo {:src "/assets/icons/dank-logo.svg"}]
           [:img.arrow-icon {:src "/assets/icons/arrow-white-right.svg"}]]]
         ]))))


(defn submit-panels [{:keys [deposit max-total-supply] :as params}]
  (let [deposit-value (:value deposit)
        all-tags-subs (subscribe [::gql/query {:queries [[:search-tags [[:items [:tag/name]]]]]}])
        form-data (r/atom {:issuance 1})
        max-tags-allowed 6
        errors (reaction {:local (let [{:keys [title issuance file-info]} @form-data
                                       entered-tag (get @form-data "txt-:search-tags")
                                       max-issuance (or (:value max-total-supply) 1)]
                                   (cond-> {:issuance {:hint (str "Max " max-issuance)}}
                                     (str/blank? title)
                                     (assoc-in [:title :error] "Title cannot be empty")

                                     (not file-info)
                                     (assoc-in [:file-info :error] "No file selected")

                                     (not (try
                                            (let [issuance (parsers/parse-int issuance)]
                                              (and (< 0 issuance) (<= issuance max-issuance)))
                                            (catch js/Error e nil)))
                                     (assoc-in [:issuance :error] (str "Issuance should be a number between 1 and " max-issuance))

                                     (> (count (get @form-data :search-tags)) max-tags-allowed)
                                     (assoc-in [:search-tags :error] (str "Max tags allowed " max-tags-allowed))

                                     (and (not (nil? entered-tag))
                                          (not (= 0 (compare
                                                     entered-tag
                                                     (apply str (re-seq #"[a-zA-Z0-9]" entered-tag))))))
                                     (assoc-in [:search-tags :error] "Only alphanumeric characters are allowed")

                                     (not= 0 (compare (count (get @form-data :search-tags))
                                                   (count (filter
                                                           #(= 0 (compare
                                                                  %
                                                                  (apply str (re-seq #"[a-zA-Z0-9]" %))))
                                                           (get @form-data :search-tags)))))
                                     (assoc-in [:search-tags :error] "Only alphanumeric characters are allowed")))
                          :remote (let [{:keys [:file-info :search-tags]} @form-data]
                                    (cond-> {}
                                      (:error file-info)
                                      (assoc-in [:file-info] (:error file-info))

                                      (:error search-tags)
                                      (assoc-in [:search-tags] (:error search-tags))))})
        critical-errors (reaction (index-by-type @errors :error))
        account-balance (subscribe [::balance-subs/active-account-balance :DANK])
        active-account @(subscribe [::accounts-subs/active-account])
        tx-id (str active-account "upload-meme" (random-uuid))
        tx-pending? (subscribe [::tx-id-subs/tx-pending? {::meme-factory/approve-and-create-meme tx-id}])
        tx-success? (subscribe [::tx-id-subs/tx-success? {::meme-factory/approve-and-create-meme tx-id}])]
    (fn []
      [app-layout
       {:meta {:title "MemeFactory - Submit to Dank Registry"
               :description "Submit a new meme to the registry for consideration. MemeFactory is decentralized registry and marketplace for the creation, exchange, and collection of provably rare digital assets."}}
       [:div.dank-registry-submit-page
        [:section.submit-header
         [header]]
        [:section.upload
         [:div.image-panel
          [file-drag-input {:form-data form-data
                            :id :file-info
                            :errors errors
                            :label "Upload a file"
                            :comment "Upload image with ratio 2:3 and size less than 1.5MB"
                            :file-accept-pred (fn [{:keys [name type size] :as props}]
                                                (log/debug "Veryfing acceptance of file" {:name name :type type :size size})
                                                (and (#{"image/png" "image/gif" "image/jpeg" "image/svg+xml" "video/mp4"} type)
                                                     (< size 1500000)))
                            :on-file-accepted (fn [{:keys [name type size array-buffer] :as props}]
                                                (swap! form-data update-in [:file-info] dissoc :error)
                                                (log/info "Accepted file" {:name name :type type :size size} ::file-accepted))
                            :on-file-rejected (fn [{:keys [name type size] :as props}]
                                                (swap! form-data assoc :file-info {:error "Non .png .jpeg .gif .svg or .mp4 file selected with size less than 1.5 Mb"})
                                                (log/warn "Rejected file" {:name name :type type :size size :user {:id active-account}} ::file-rejected))}]]
         [:div.form-panel
          [with-label "Title"
           [text-input {:form-data form-data
                        :errors errors
                        :id :title
                        :dom-id :ftitle
                        :maxLength 60}]
           {:form-data form-data
            :id :title
            :for :ftitle}]
          [with-label "Tags"
           [chip-input {:form-data form-data
                        :chip-set-path [:search-tags]
                        :ac-options (->> @all-tags-subs :search-tags :items (mapv :tag/name))
                        :chip-render-fn (fn [c] [:span c])
                        :id :search-tags
                        :select-keycodes #{13 188 32}
                        :errors errors
                        :dom-id :search-tags}]
           {:form-data form-data
            :for :search-tags
            :id :search-tags}]
          [with-label "Issuance"
           [text-input {:form-data form-data
                        :errors errors
                        :id :issuance
                        :dom-id :issuance
                        :type :number
                        :min 1}]
           {:form-data form-data
            :id :issuance
            :for :issuance}]
          [:div.comment
           [:label "Comment"]
           [text-input {:form-data form-data
                        :id :comment
                        :errors errors
                        :class "comment"
                        :input-type :textarea
                        :dom-id :comment
                        :maxLength 500}]]
          #_[:span.max-issuance (str "Max " max-meme-issuance)] ;; we are showing it on input focus
          [:div.submit
           [chain-check-pending-button {:pending? @tx-pending?
                                        :disabled (or @tx-pending? @tx-success? (not (empty? @critical-errors))
                                                      (< @account-balance deposit-value) (not active-account))
                                        :pending-text "Submitting ..."
                                        :on-click #(dispatch [::dr-events/upload-meme
                                                              {:send-tx/id tx-id
                                                               :form-data @form-data
                                                               :deposit deposit-value}])}
            (if @tx-success?
              "Submitted"
              "Submit")]
           [dank-with-logo (web3/from-wei deposit-value :ether)]]
          (when (< @account-balance deposit-value)
            [:div.not-enough-dank "You don't have enough DANK tokens to submit a meme"])]]]])))


(defmethod page :route.dank-registry/submit []
  (let [params @(subscribe [:memefactory.ui.config/memefactory-db-params])]
    (when params
      [submit-panels params])))
