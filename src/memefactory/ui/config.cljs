(ns memefactory.ui.config
  (:require [ajax.core :as ajax]
            [district.graphql-utils :as graphql-utils]
            [district.ui.logging.events :as logging]
            [district.ui.smart-contracts.queries :as contract-queries]
            [eip55.core :refer [address->checksum]]
            [graphql-query.core :refer [graphql-query]]
            [memefactory.shared.graphql-schema :refer [graphql-schema]]
            [memefactory.shared.smart-contracts-dev :as smart-contracts-dev]
            [memefactory.shared.smart-contracts-prod :as smart-contracts-prod]
            [memefactory.shared.smart-contracts-qa :as smart-contracts-qa]
            [mount.core :refer [defstate]]
            [re-frame.core :as re-frame]
            [taoensso.timbre :as log])
  (:require-macros [memefactory.shared.utils :refer [get-environment]]))

(def skipped-contracts [:ds-guard :meme-auth :minime-token-factory])

(def development-config
  {:debug? true
   :logging {:level :debug
             :console? true}
   :time-source :js-date
   :smart-contracts {:contracts (apply dissoc smart-contracts-dev/smart-contracts skipped-contracts)}
   :web3-accounts {:eip55? true}
   :web3-balances {:contracts (select-keys smart-contracts-dev/smart-contracts [:DANK :DANK-root])}
   :web3-chain {:l1 {:chain-id "1337"}
                :l2 {:chain-id "15001"
                     :rpc-urls ["http://localhost:8545"]
                     :chain-name "Bor"
                     :native-currency {:name "Matic"
                                       :symbol "MATIC"
                                       :decimals 18}}
                :deployed-on :l2}
   :web3 {:url "http://localhost:9545"}
   :web3-tx {:disable-loading-recommended-gas-prices? true
             :eip55? true}
   :web3-tx-log {:disable-using-localstorage? true
                 :open-on-tx-hash? true
                 :tx-costs-currencies [:USD]
                 :etherscan-url "https://mumbai.polygonscan.com"}
   :graphql {:schema graphql-schema
             :url "http://localhost:6300/graphql"}
   :ipfs {:endpoint "/api/v0"
          :host "http://127.0.0.1:5001"
          :gateway "http://127.0.0.1:8080/ipfs"}
   :router {:html5? false}
   :router-google-analytics {:enabled? false}})

(def qa-config
  {:logging {:level :warn
             :console? true
            ;;  :sentry {:dsn "https://4bb89c9cdae14444819ff0ac3bcba253@sentry.io/1306960"
            ;;           :environment "QA"}
                      }
   :time-source :js-date
   :smart-contracts {:contracts (apply dissoc smart-contracts-qa/smart-contracts skipped-contracts)
                     :load-method :use-loaded}
   :web3-accounts {:eip55? true}
   :web3-balances {:contracts (select-keys smart-contracts-qa/smart-contracts [:DANK :DANK-root])}
   :web3-chain {:l1 {:chain-id "5"}
                :l2 {:chain-id "80001"
                     :rpc-urls ["https://rpc-mumbai.maticvigil.com"]
                     :chain-name "Mumbai"
                     :native-currency {:name "Matic"
                                       :symbol "MATIC"
                                       :decimals 18}
                     :block-explorer-urls ["https://mumbai.polygonscan.com/"]}
                :deployed-on :l2}
   :web3 {:url "https://ropsten.infura.io"}
   :web3-tx {:eip55? true}
   :web3-tx-log {:disable-using-localstorage? false
                 :open-on-tx-hash? true
                 :tx-costs-currencies [:USD]
                 :etherscan-url "https://mumbai.polygonscan.com"}
   :graphql {:schema graphql-schema
             :url "https://api.memefactory.qa.district0x.io/graphql"}
   :ipfs {:host "https://ipfs.qa.district0x.io"
          :endpoint "/api/v0"
          :gateway "https://ipfs.qa.district0x.io/gateway/ipfs"}
   :router {:html5? true}
   :router-google-analytics {:enabled? false}})

(def qa-dev-config (assoc-in qa-config [:router :html5?] false))

(def production-config
  {:logging {:level :warn
             :console? false
             :sentry {:dsn "https://4bb89c9cdae14444819ff0ac3bcba253@sentry.io/1306960"
                      :environment "PRODUCTION"}}
   :time-source :blockchain
   :smart-contracts {:contracts (apply dissoc smart-contracts-prod/smart-contracts skipped-contracts)
                     :load-method :use-loaded}
   :web3-accounts {:eip55? true}
   :web3-balances {:contracts (select-keys smart-contracts-prod/smart-contracts [:DANK :DANK-root])}
   :web3 {:url "https://mainnet.infura.io"}
   :web3-chain {:l1 {:chain-id "1"}
                :l2 {:chain-id "137"
                     :rpc-urls ["https://matic-mainnet.chainstacklabs.com"]
                     :chain-name "Polygon"
                     :native-currency {:name "Matic"
                                       :symbol "MATIC"
                                       :decimals 18}
                     :block-explorer-urls ["https://polygonscan.com/"]
                     }
                :deployed-on :l2}
   :web3-tx {:eip55? true}
   :web3-tx-log {:disable-using-localstorage? false
                 :open-on-tx-hash? true
                 :tx-costs-currencies [:USD]
                 :etherscan-url "https://polygonscan.com"}
   :graphql {:schema graphql-schema
             :url "https://api.memefactory.io/graphql"}
   :ipfs {:host "https://ipfs.district0x.io"
          :endpoint "/api/v0"
          :gateway "https://ipfs.district0x.io/gateway/ipfs"}
   :router {:html5? true}
   :router-google-analytics {:enabled? true}})

(def config-map
  (condp = (get-environment)
    "prod" production-config
    "qa" qa-config
    "qa-dev" qa-dev-config
    "dev" development-config))

(defn start []
  (re-frame/dispatch [::load-memefactory-db-params])
  (re-frame/dispatch [::load-param-change-db-params]))

(defn load-db-params [db {:keys [on-success]}]
  (let [graphql-url (-> config-map :graphql :url)
        db-address (address->checksum db)
        _ (log/debug "Loading param change initial parameters" {:graphql-url graphql-url
                                                                :db db-address})
        query (graphql-query {:queries [[:params {:db db-address
                                                  :keys [(graphql-utils/kw->gql-name :max-auction-duration)
                                                         (graphql-utils/kw->gql-name :max-total-supply)
                                                         (graphql-utils/kw->gql-name :reveal-period-duration)
                                                         (graphql-utils/kw->gql-name :commit-period-duration)
                                                         (graphql-utils/kw->gql-name :challenge-dispensation)
                                                         (graphql-utils/kw->gql-name :challenge-period-duration)
                                                         (graphql-utils/kw->gql-name :deposit)]}
                                         [:param/key :param/value :param/set-on :param/db]]]}
                             {:kw->gql-name graphql-utils/kw->gql-name})]
    {:http-xhrio {:method          :post
                  :uri             graphql-url
                  :params          {:query query}
                  :timeout         8000
                  :response-format (ajax/json-response-format {:keywords? true})
                  :format          (ajax/json-request-format)
                  :on-success      on-success
                  :on-failure      [::logging/error "Error loading initial parameters" graphql-url ::load-initial-params]}}))

(re-frame/reg-event-fx
 ::load-memefactory-db-params
 (fn [{:keys [:db]} _]
   (load-db-params (contract-queries/contract-address db :meme-registry-db)
                   {:on-success [::db-params-loaded ::memefactory-db-params]})))

(re-frame/reg-event-fx
 ::load-param-change-db-params
 (fn [{:keys [:db]} _]
   (load-db-params (contract-queries/contract-address db :param-change-registry-db)
                   {:on-success [::db-params-loaded ::param-change-db-params]})))

(re-frame/reg-event-db
 ::db-params-loaded
 (fn [db [_ key initial-params-result]]
   (let [params-map (->> initial-params-result :data :params
                         (map (fn [entry] [(graphql-utils/gql-name->kw (:param_key entry)) {:value (:param_value entry)
                                                                                            :set-on (:param_setOn entry)
                                                                                            :db (:param_db entry)}]))
                         (into {}))]
     (log/debug "Initial parameters" params-map)
     (assoc db key (merge
                    params-map)))))

(re-frame/reg-sub
 ::memefactory-db-params
 (fn [db _]
   (::memefactory-db-params db)))

(re-frame/reg-sub
 ::param-change-db-params
 (fn [db _]
   (::param-change-db-params db)))

(re-frame/reg-sub
 ::all-params
 (fn [db _]
   (when (and (::memefactory-db-params db) (::param-change-db-params db))
     (concat
      (->> (::memefactory-db-params db)
           (map (fn [[k pm]] (assoc pm :key (keyword "meme" k)))))
      (->> (::param-change-db-params db)
           (map (fn [[k pm]] (assoc pm :key (keyword "param-change" k)))))))))

(re-frame/reg-sub
 ::param-db-keys-by-db
 (fn [db _]
   {(-> (::param-change-db-params db) first second :db) :param-change-registry-db
    (-> (::memefactory-db-params db) first second :db)  :meme-registry-db}))

(defstate config
  :start (start)
  :stop ::stopped)
