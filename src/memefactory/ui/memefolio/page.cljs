(ns memefactory.ui.memefolio.page
  (:require
   [clojure.string :as str]
   [district.ui.component.page :refer [page]]
   [district.ui.component.input :as input]
   [district.ui.graphql.subs :as gql]
   [district.ui.web3-accounts.subs :as accounts-subs]
   [memefactory.ui.components.app-layout :refer [app-layout]]
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as r]
   [print.foo :refer [look] :include-macros true]))

(def default-tab :curated)

(defn resolve-image [meta-hash]
  "http://upload.wikimedia.org/wikipedia/en/thumb/6/63/Feels_good_man.jpg/200px-Feels_good_man.jpg")

(defmulti panel (fn [tab & opts] tab))

(defmethod panel :collected [_ active-account]
  (let [collected (subscribe [::gql/query
                              {:queries [[:search-memes {:owner active-account}
                                          [[:items [:reg-entry/address
                                                    :meme/meta-hash
                                                    :meme/number
                                                    :meme/title
                                                    [:meme/owned-meme-tokens {:owner active-account}
                                                     [:meme-token/token-id]]
                                                    :meme/total-supply]]
                                           :total-count]]]}])]
    (fn []
      (if (:graphql/loading? @collected)
        [:div "Loading..."]
        [:div.tiles
         (map (fn [{:keys [:reg-entry/address :meme/meta-hash :meme/number
                           :meme/title :meme/total-supply :meme/owned-meme-tokens] :as meme}]
                (when address
                  (do ^{:key address} [:div.meme-card-front {:style {:width 200
                                                                     :height 280
                                                                     :display "block"}}
                                       [:img {:src (resolve-image meta-hash)}]
                                       [:div [:b (str "#" number " " title)]]
                                       [:div [:span (str "Owning " (count owned-meme-tokens) " out of " total-supply)]]])))
              (-> @collected :search-memes :items))]))))

(defmethod panel :created [_ active-account]
  (let [created (subscribe [::gql/query
                            {:queries [[:search-memes {:creator active-account}
                                        [[:items [:reg-entry/address
                                                  :meme/meta-hash
                                                  :meme/number
                                                  :meme/title
                                                  :meme/total-minted
                                                  :meme/total-supply
                                                  :reg-entry/status]]
                                         :total-count]]]}])
        input-value (r/atom "0")]
    (fn []
      (if (:graphql/loading? @created)
        [:div "Loading..."]
        [:div.tiles
         (doall (map (fn [{:keys [:reg-entry/address :meme/meta-hash :meme/number
                                  :meme/title :meme/total-supply :meme/total-minted
                                  :reg-entry/status] :as meme}]
                       (when address
                         (let [max-value (- total-supply total-minted)
                               valid? #(let [input (js/Number %)]
                                         (and (not (js/isNaN input))
                                              (look (int? (look input)))
                                              (<= input max-value)))]
                           ^{:key address} [:div.meme-card-front {:style {:width 200
                                                                          :height 280
                                                                          :display "block"}}
                                            [:img {:src (resolve-image meta-hash)}]
                                            [:div [:b (str "#" number " " title)]]
                                            [:div [:span (str total-minted "/" total-supply" Issued")]]
                                            (when (= status "regEntry_status_whitelisted")
                                              [:div {:style {:margin-top 10}}
                                               [input/input
                                                {:label "Issue"
                                                 :fluid true
                                                 :value @input-value
                                                 :error (not (valid? @input-value))
                                                 :on-change #(reset! input-value (aget %2 "value"))}]
                                               [:div [:span (str "Max " max-value)]]])])))
                     (-> @created :search-memes :items)))]))))

(defmethod panel :curated [_ active-account]
  (let [curated (subscribe [::gql/query
                            {:queries [[:search-memes {:curator active-account}
                                        [[:items [:reg-entry/address
                                                  :meme/meta-hash
                                                  :meme/number
                                                  :meme/title
                                                  [:challenge/vote {:vote/voter active-account}
                                                   [:vote/option]]]]
                                         :total-count]]]}])]
    (fn []
      (if (:graphql/loading? @curated)
        [:div "Loading..."]
        [:div.tiles
         (map (fn [{:keys [:reg-entry/address :meme/meta-hash :meme/number
                           :meme/title :challenge/vote] :as meme}]
                (when address
                  (let [{:keys [:vote/option]} (look vote)]
                    ^{:key address} [:div.meme-card-front {:style {:width 200
                                                                   :height 280
                                                                   :display "block"}}
                                     [:img {:src (resolve-image meta-hash)}]
                                     [:div [:b (str "#" number " " title)]]
                                     [:div
                                      (cond
                                        (= option "voteOption_noVote")
                                        [:label
                                         [:b "Voted Unrevealed"]]

                                        (= option "voteOption_voteFor")
                                        [:label "Voted Dank"
                                         [:i.icon.thumbs.up.outline]]

                                        (= option "voteOption_voteAgainst")
                                        [:label
                                         [:b "Voted Stank"]
                                         [:i.icon.thumbs.down.outline]])]])))
              (-> @curated :search-memes :items))]))))

(defmethod panel :selling [_ active-account]
  (fn []
    [:div "SELING"]))

(defmethod panel :sold [_ active-account]
  (fn []
    [:div "SOLD"]))

(defn tabbed-pane []
  (let [tab (r/atom default-tab)
        active-account (subscribe [::accounts-subs/active-account])]
    (fn []
      [:div.tabbed-pane
       [:div.tabs
        (map (fn [tab-id]
               ^{:key tab-id} [:label
                               {:on-click (fn [evt]
                                            (reset! tab tab-id))}
                               [:a (-> tab-id
                                       name
                                       (str/capitalize))]])
             [:collected :created :curated :selling :sold])]

       [:div.panel
        [(panel @tab @active-account)]]])))

(defmethod page :route.memefolio/index []
  (let [search-atom (r/atom {:term ""})]
    (fn []
      [app-layout
       {:meta {:title "MemeFactory"
               :description "Description"}
        :search-atom search-atom}
       [:div.memefolio
        [tabbed-pane]]])))
