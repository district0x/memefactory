(ns memefactory.ui.core
  (:require
   [cljs.spec.alpha :as s]
   [clojure.string :as str]
   [district.ui.component.router :refer [router]]
   [district.ui.graphql]
   [district.ui.logging]
   [district.ui.notification]
   [district.ui.now]
   [district.ui.reagent-render]
   [district.ui.router-google-analytics]
   [district.ui.router]
   [district.ui.smart-contracts]
   [district.ui.web3-account-balances]
   [district.ui.web3-accounts]
   [district.ui.web3-balances]
   [district.ui.web3-sync-now]
   [district.ui.web3-tx-id]
   [district.ui.web3-tx-log]
   [district.ui.web3-tx]
   [district.ui.web3]
   [district.ui.window-size]
   [memefactory.shared.contract.registry-entry]
   [memefactory.shared.graphql-schema :refer [graphql-schema]]
   [memefactory.shared.routes :refer [routes]]
   [memefactory.shared.smart-contracts :refer [smart-contracts]]
   [memefactory.ui.config :as config]
   [memefactory.ui.config :refer [config-map]]
   [memefactory.ui.contract.registry-entry]
   [memefactory.ui.dank-registry.browse-page]
   [memefactory.ui.dank-registry.challenge-page]
   [memefactory.ui.dank-registry.submit-page]
   [memefactory.ui.dank-registry.vote-page]
   [memefactory.ui.get-dank.page]
   [memefactory.ui.home.page]
   [memefactory.ui.ipfs]
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
   [re-frisk.core :refer [enable-re-frisk!]]
   ))

(def skipped-contracts [:ds-guard :param-change-registry-db :meme-registry-db :minime-token-factory])

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
               :votes (:votes store))}))

(defn ^:export init []
  (dev-setup)
  (let [full-config (merge config-map
                           {:smart-contracts {:contracts (apply dissoc smart-contracts skipped-contracts)
                                              :format :truffle-json}
                            :web3-balances {:contracts (select-keys smart-contracts [:DANK])}
                            :web3-account-balances {:for-contracts [:ETH :DANK]}
                            :reagent-render {:id "app"
                                             :component-var #'router}
                            :router {:routes routes
                                     :default-route :route/home}
                            :router-google-analytics {:enabled? nil #_(not debug?)}
                            :district-ui-notification {:default-show-duration 2000
                                                       :default-hide-duration 1000}})]
    (js/console.log "Entire config:" (clj->js full-config))
    (-> (mount/with-args full-config)
        (mount/start)))

  (re-frame/dispatch-sync [::init]))
