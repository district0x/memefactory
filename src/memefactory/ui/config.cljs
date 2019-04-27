(ns memefactory.ui.config
  (:require
   [ajax.core :as ajax]
   [district.graphql-utils :as graphql-utils]
   [district.ui.logging.events :as logging]
   [graphql-query.core :refer [graphql-query]]
   [memefactory.shared.graphql-schema :refer [graphql-schema]]
   [memefactory.shared.smart-contracts-dev :as smart-contracts-dev]
   [memefactory.shared.smart-contracts-prod :as smart-contracts-prod]
   [memefactory.shared.smart-contracts-qa :as smart-contracts-qa]
   [mount.core :refer [defstate]]
   [re-frame.core :as re-frame]
   [taoensso.timbre :as log])
  (:require-macros [memefactory.shared.utils :refer [get-environment]]))

(def skipped-contracts [:ds-guard :param-change-registry-db :meme-registry-db :minime-token-factory])

(def development-config
  {:debug? true
   :logging {:level :debug
             :console? true}
   :time-source :js-date
   :smart-contracts {:contracts (apply dissoc smart-contracts-dev/smart-contracts skipped-contracts)}
   :web3-balances {:contracts (select-keys smart-contracts-dev/smart-contracts [:DANK])}
   :web3 {:url "http://localhost:8549"}
   :web3-tx-log {:disable-using-localstorage? true
                 :open-on-tx-hash? true
                 :tx-costs-currencies [:USD]
                 :etherscan-url "https://ropsten.etherscan.io"}
   :graphql {:schema graphql-schema
             :url "http://localhost:6300/graphql"}
   :ipfs {:host "http://127.0.0.1:5001"
          :endpoint "/api/v0"
          :gateway "http://127.0.0.1:8080/ipfs"}
   :router {:html5? false}
   :router-google-analytics {:enabled? false}})

(def qa-config
  {:logging {:level :warn
             :console? true
             :sentry {:dsn "https://4bb89c9cdae14444819ff0ac3bcba253@sentry.io/1306960"
                      :environment "QA"}}
   :time-source :js-date
   :smart-contracts {:contracts (apply dissoc smart-contracts-qa/smart-contracts skipped-contracts)}
   :web3-balances {:contracts (select-keys smart-contracts-qa/smart-contracts [:DANK])}
   :web3 {:url "http://ropsten.district0x.io"}
   :web3-tx-log {:disable-using-localstorage? false
                 :open-on-tx-hash? true
                 :tx-costs-currencies [:USD]
                 :etherscan-url "https://ropsten.etherscan.io"}
   :graphql {:schema graphql-schema
             :url "http://api.memefactory.qa.district0x.io/graphql"}
   :ipfs {:host "http://ipfs.qa.district0x.io/api"
          :endpoint "/api/v0"
          :gateway "http://ipfs.qa.district0x.io/gateway/ipfs"}
   :router {:html5? true}
   :router-google-analytics {:enabled? false}})

(def qa-dev-config (assoc-in qa-config [:router :html5?] false))

(def production-config
  {:logging {:level :warn
             :console? false
             :sentry {:dsn "https://4bb89c9cdae14444819ff0ac3bcba253@sentry.io/1306960"
                      :environment "PRODUCTION"}}
   :time-source :js-date
   :smart-contracts {:contracts (apply dissoc smart-contracts-prod/smart-contracts skipped-contracts)}
   :web3-balances {:contracts (select-keys smart-contracts-prod/smart-contracts [:DANK])}
   :web3 {:url "https://mainnet.district0x.io"}
   :web3-tx-log {:disable-using-localstorage? false
                 :open-on-tx-hash? true
                 :tx-costs-currencies [:USD]
                 :etherscan-url "https://etherscan.io"}
   :graphql {:schema graphql-schema
             :url "https://api.memefactory.io/graphql"}
   :ipfs {:host "https://ipfs.district0x.io/api"
          :endpoint "/api/v0"
          :gateway "http://ipfs.district0x.io/gateway/ipfs"}
   :router {:html5? true}
   :router-google-analytics {:enabled? true}})

(def config-map
  (condp = (get-environment)
    "prod" production-config
    "qa" qa-config
    "qa-dev" qa-dev-config
    "dev" development-config))

(defn start []
  (re-frame/dispatch [::load-memefactory-db-params (-> config-map :graphql :url)]))

(re-frame/reg-event-fx
 ::load-memefactory-db-params
 (fn [cofx [_ graphql-url]]
   (log/debug "Loading initial parameters" {:graphql-url graphql-url})
   (let [query (graphql-query {:queries [[:params {:db (graphql-utils/kw->gql-name :meme-registry-db)
                                                   :keys [(graphql-utils/kw->gql-name :max-auction-duration)
                                                          (graphql-utils/kw->gql-name :vote-quorum)
                                                          (graphql-utils/kw->gql-name :max-total-supply)
                                                          (graphql-utils/kw->gql-name :reveal-period-duration)
                                                          (graphql-utils/kw->gql-name :commit-period-duration)
                                                          (graphql-utils/kw->gql-name :challenge-dispensation)
                                                          (graphql-utils/kw->gql-name :challenge-period-duration)
                                                          (graphql-utils/kw->gql-name :deposit)]}
                                          [:param/key :param/value]]]}
                              {:kw->gql-name graphql-utils/kw->gql-name})]
     {:http-xhrio {:method          :post
                   :uri             graphql-url
                   :params          {:query query}
                   :timeout         8000
                   :response-format (ajax/json-response-format {:keywords? true})
                   :format          (ajax/json-request-format)
                   :on-success      [::memefactory-db-params-loaded]
                   :on-failure      [::logging/error "Error loading initial parameters" graphql-url ::load-initial-params]}})))

(re-frame/reg-event-db
 ::memefactory-db-params-loaded
 (fn [db [_ initial-params-result]]
   (let [params-map (->> initial-params-result :data :params
                         (map (fn [entry] [(graphql-utils/gql-name->kw (:param_key entry)) (:param_value entry)]))
                         ;; limit hardcoded in MemeAuction.sol startAuction
                         (into {:min-auction-duration 60}))]
     (log/debug "Initial parameters" params-map)
     (assoc db ::memefactory-db-params (merge
                                        params-map)))))

(re-frame/reg-sub
 ::memefactory-db-params
 (fn [db _]
   (::memefactory-db-params db)))

(defstate config
  :start (start)
  :stop ::stopped)
