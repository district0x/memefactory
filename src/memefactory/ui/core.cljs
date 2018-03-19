(ns memefactory.ui.core
  (:require
    [cljs.spec.alpha :as s]
    [clojure.string :as str]
    [district.ui.component.router :refer [router]]
    [district.ui.notification]
    [district.ui.now]
    [district.ui.reagent-render]
    [district.ui.router-google-analytics]
    [district.ui.router]
    [district.ui.smart-contracts]
    [district.ui.web3-account-balances]
    [district.ui.web3-accounts]
    [district.ui.web3-balances]
    [district.ui.web3-tx-id]
    [district.ui.web3-tx-log]
    [district.ui.web3-tx]
    [district.ui.web3]
    [district.ui.window-size]
    [memefactory.shared.graphql-schema :refer [graphql-schema]]
    [memefactory.shared.graphql-utils :as graphql-utils]
    [memefactory.shared.routes :refer [routes]]
    [memefactory.shared.smart-contracts :refer [smart-contracts]]
    [memefactory.ui.graphql.events :as graphql-events]
    [memefactory.ui.graphql]
    [memefactory.ui.home.page]
    [mount.core :as mount]
    [print.foo :include-macros true]
    [venia.core :as v]))

(def debug? ^boolean js/goog.DEBUG)

(def skipped-contracts [:ds-guard :param-change-registry-db :meme-registry-db :minime-token-factory])

(defn ^:export init []
  (s/check-asserts debug?)
  (enable-console-print!)
  (-> (mount/with-args
        {:web3 {:url "http://localhost:8549"}
         :smart-contracts {:contracts (apply dissoc smart-contracts skipped-contracts)}
         :web3-balances {:contracts (select-keys smart-contracts [:DANK])}
         :web3-tx-log {:open-on-tx-hash? true
                       :tx-costs-currencies [:USD]}
         :reagent-render {:id "app"
                          :component-var #'router}
         :router {:routes routes
                  :default-route :route/home}
         :router-google-analytics {:enabled? (not debug?)}
         :graphql {:schema graphql-schema
                   :url "http://localhost:6300/graphql"}})
    (mount/start)))


(re-frame.core/reg-event-fx
  ::print
  (fn [{:keys [:db]} [_ & args]]
    (print.foo/look args)
    nil))

(comment
  (re-frame.core/dispatch [::graphql-events/query
                           {:query {:venia/queries [{:query/data [:search-memes
                                                                  {:a 1}
                                                                  :fragment/comparisonFields
                                                                  #_[[:items [:reg-entry/created-on]]]
                                                                  ]
                                                     :query/alias :kek}]
                                    :venia/fragments [{:fragment/name "comparisonFields"
                                                       :fragment/type :MemeList
                                                       :fragment/fields [[:items [:reg-entry/created-on]]]}]}
                            :on-success [::print]
                            :on-error [::print]}])

  (re-frame.core/dispatch [::graphql-events/query
                           {:query {:venia/operation {:operation/type :query
                                                      :operation/name "employeeQuery"}
                                    :venia/queries [{:query/data [:search-memes
                                                                  {:a 1}
                                                                  [:total-count
                                                                   [:items
                                                                    [:reg-entry/address
                                                                     :meta/typename
                                                                     :reg-entry/created-on
                                                                     [:challenge/available-vote-amount {:voter "0x987"}]
                                                                     [:challenge/vote {:vote/voter "0x987"}
                                                                      [:vote/amount :vote/option]]
                                                                     {:field/alias :loool
                                                                      :field/data [[:challenge/vote {:vote/voter "0x980"}
                                                                                    [:vote/amount :vote/option]]]}]]]]
                                                     :query/alias :kek}
                                                    [:search-memes
                                                     {:a 2}
                                                     [[:items [:reg-entry/address]]]]
                                                    [:meme {:reg-entry/address "0x000"}
                                                     [:reg-entry/created-on]]
                                                    [:param {:db "0Fx123" :key "a"}
                                                     [:param/key :param/value]]
                                                    [:params {:db "0x123" :keys ["a" "b"]}
                                                     [:param/key :param/value]]]}
                            :variables {:a 1}
                            :on-success [::print]
                            :on-error [::print]}])

  (re-frame.core/dispatch [::graphql-events/query
                           {:query {:venia/queries [{:query/data [:employee {:id 1
                                                                             :active true
                                                                             :name "asda"}
                                                                  :fragment/comparisonFields]
                                                     :query/alias :workhorse}
                                                    {:query/data [:employee {:id 2
                                                                             :active false}
                                                                  :fragment/comparisonFields]
                                                     :query/alias :boss}]
                                    :venia/fragments [{:fragment/name "comparisonFields"
                                                       :fragment/type :Worker
                                                       :fragment/fields [:name :address [:friends [:name :email]]]}]}
                            :variables {:id 9 :name "somename"}
                            :on-success [::print]
                            :on-error [::print]}])

  (re-frame.core/dispatch [::graphql-events/query
                           {:query {:venia/operation {:operation/type :query
                                                      :operation/name "employeeQuery"}
                                    :venia/variables [{:variable/name "id"
                                                       :variable/type :Int
                                                       :variable/default 1}
                                                      {:variable/name "name"
                                                       :variable/type :String}]
                                    :venia/queries [{:query/data [:employee {:id :$id
                                                                             :active true
                                                                             :name :$name}
                                                                  :fragment/comparisonFields]
                                                     :query/alias :workhorse}
                                                    {:query/data [:employee {:id :$id
                                                                             :active false}
                                                                  :fragment/comparisonFields]
                                                     :query/alias :boss}]
                                    :venia/fragments [{:fragment/name "comparisonFields"
                                                       :fragment/type :Worker
                                                       :fragment/fields [:name :address [:friends [:name :email]]]}]}
                            :variables {:id 9 :name "somename"}
                            :on-success [::print]
                            :on-error [::print]}])

  (re-frame.core/dispatch [::graphql-events/query
                           {:query {:venia/operation {:operation/type :mutation
                                                      :operation/name "AddProjectToEmployee"}
                                    :venia/variables [{:variable/name "id"
                                                       :variable/type :Int!}]
                                    :venia/queries [[:addProject {:employeeId :$id}
                                                     [:allocation :name]]]}
                            :variables {:id 55}
                            :on-success [::print]
                            :on-error [::print]}]))