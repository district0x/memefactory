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
   [district.ui.smart-contracts]
   [district.ui.web3-account-balances]
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
   [memefactory.ui.privacy-policy.page]
   [memefactory.ui.terms.page]
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
   [memefactory.ui.my-settings.page]
   [memefactory.ui.subs]
   [mount.core :as mount]
   [print.foo :refer [look] :include-macros true]
   [re-frame.core :as re-frame]
   [re-frisk.core :refer [enable-re-frisk!]]))

;; (def skipped-contracts [:ds-guard :param-change-registry-db :meme-registry-db :minime-token-factory])

(defn dev-setup []
  (when (:debug? @config/config)
    (enable-console-print!)
    (enable-re-frisk!)))


(re-frame/reg-event-fx
 ::init
 [(re-frame/inject-cofx :store)]
 (fn [{:keys [db store]}]
   {:db (assoc db
               :settings (:settings store)
               :votes (:votes store)
               :memefactory.ui.get-dank.page/stage 1)}))


(defn ^:export init []
  (dev-setup)
  (let [full-config (cljs-utils/merge-in
                      config-map
                      {:smart-contracts {:format :truffle-json}
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
