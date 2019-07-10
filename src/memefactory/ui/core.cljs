(ns memefactory.ui.core
  (:require
   [cljsjs.jquery]
   [district.cljs-utils :as cljs-utils]
   [district.ui.component.router :refer [router]]
   [district.ui.graphql]
   [district.ui.ipfs]
   [district.ui.logging]
   [district.ui.mobile]
   [district.ui.notification]
   [district.ui.now]
   [district.ui.reagent-render]
   [district.ui.router-google-analytics]
   [district.ui.router]
   [district.ui.smart-contracts.events :as contracts-events]
   [district.ui.smart-contracts]
   [district.ui.web3-account-balances]
   [district.ui.web3-accounts.events :as web3-accounts-events]
   [district.ui.web3-accounts]
   [district.ui.web3-balances]
   [district.ui.web3-tx-id]
   [district.ui.web3-tx-log]
   [district.ui.web3-tx]
   [district.ui.web3]
   [district.ui.window-size]
   [memefactory.shared.contract.registry-entry]
   [memefactory.shared.graphql-schema :refer [graphql-schema]]
   [memefactory.shared.routes :refer [routes]]
   [memefactory.ui.about.page]
   [memefactory.ui.config :as config]
   [memefactory.ui.config :refer [config-map]]
   [memefactory.ui.contract.registry-entry]
   [memefactory.ui.dank-registry.browse-page]
   [memefactory.ui.dank-registry.challenge-page]
   [memefactory.ui.dank-registry.submit-page]
   [memefactory.ui.dank-registry.vote-page]
   [memefactory.ui.get-dank.page]
   [memefactory.ui.home.page]
   [memefactory.ui.how.page]
   [memefactory.ui.leaderboard.collectors-page]
   [memefactory.ui.leaderboard.creators-page]
   [memefactory.ui.leaderboard.curators-page]
   [memefactory.ui.leaderboard.dankest-page]
   [memefactory.ui.marketplace.page]
   [memefactory.ui.meme-detail.page]
   [memefactory.ui.memefolio.page]
   [memefactory.ui.my-settings.events :as my-settings-events]
   [memefactory.ui.my-settings.page]
   [memefactory.ui.privacy-policy.page]
   [memefactory.ui.subs]
   [memefactory.ui.terms.page]
   [mount.core :as mount]
   [re-frame.core :as re-frame]
   [re-frisk.core :refer [enable-re-frisk!]]
   [taoensso.timbre :as log]
   ))

(def interceptors [re-frame/trim-v])

(defn dev-setup []
  (when (:debug? @config/config)
    (enable-console-print!)
    (enable-re-frisk!)))

(re-frame/reg-event-db
 ::init-defaults
 interceptors
 (fn [db [store]]
   (-> db
       (assoc :settings (:settings store))
       (assoc :votes (:votes store))
       (assoc :memefactory.ui.get-dank.page/stage 1))))

(re-frame/reg-event-fx
 ::init
 [(re-frame/inject-cofx :store)]
 (fn [{:keys [db store]}]
   (log/debug "Localstore content" store ::init)
   {:async-flow {:first-dispatch [::init-defaults store]
                 :rules [{:when :seen-all-of?
                          :events [::web3-accounts-events/active-account-changed
                                   ::contracts-events/contracts-loaded]
                          :dispatch [::my-settings-events/load-email-settings]}]}}))

(defn ^:export init []
  (dev-setup)
  (let [full-config (cljs-utils/merge-in
                     config-map
                     {:smart-contracts {:format :truffle-json
                                        :contracts-path "/contracts/build/"}
                      :web3-account-balances {:for-contracts [:ETH :DANK]}
                      :web3-tx-log {:open-on-tx-hash? true}
                      :reagent-render {:id "app"
                                       :component-var #'router}
                      :router {:routes routes
                               :default-route :route/home
                               :scroll-top? true}
                      :notification {:default-show-duration 3000
                                     :default-hide-duration 1000}})]
    (js/console.log "Entire config:" (clj->js full-config))
    (-> (mount/with-args full-config)
        (mount/start)))
  (re-frame/dispatch-sync [::init]))
