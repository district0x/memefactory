(ns memefactory.ui.config
  (:require
   [ajax.core :as ajax]
   [district.graphql-utils :as graphql-utils]
   [district.ui.logging.events :as logging]
   [graphql-query.core :refer [graphql-query]]
   [memefactory.shared.graphql-schema :refer [graphql-schema]]
   [mount.core :as mount :refer [defstate]]
   [re-frame.core :as re-frame])
  (:require-macros [memefactory.ui.utils :refer [get-environment]]))

(def development-config
  {:debug? true
   :logging {:level :debug
             :console? true}
   :time-source :blockchain
   :web3 {:url "http://localhost:8549"}
   :web3-tx-log {:disable-using-localstorage? true
                 :open-on-tx-hash? false
                 :tx-costs-currencies [:USD]
                 :etherscan-url "https://ropsten.etherscan.io"}
   :graphql {:schema graphql-schema
             :url "http://localhost:6300/graphql"}
   :ipfs {:host "http://127.0.0.1:5001" :endpoint "/api/v0"}})

(def qa-config
  {:logging {:level :warn
             :console? true
             :sentry {:dsn "https://4bb89c9cdae14444819ff0ac3bcba253@sentry.io/1306960"
                      :environment "QA"}}
   :time-source :js-date
   :web3 {:url "http://qa.district0x.io:8545"}
   :web3-tx-log {:disable-using-localstorage? false
                 :open-on-tx-hash? true
                 :tx-costs-currencies [:USD]
                 :etherscan-url "https://ropsten.etherscan.io"}
   :graphql {:schema graphql-schema
             :url "http://qa.district0x.io:6300/graphql"}
   :ipfs {:host "http://qa.district0x.io:5001" :endpoint "/api/v0"}})

(def production-config
  {:logging {:level :warn
             :console? false
             :sentry {:dsn "https://4bb89c9cdae14444819ff0ac3bcba253@sentry.io/1306960"
                      :environment "PRODUCTION"}}
   :time-source :js-date
   :web3 {:url "http://memefactory.io:8545"}
   :web3-tx-log {:disable-using-localstorage? false
                 :open-on-tx-hash? true
                 :tx-costs-currencies [:USD]
                 :etherscan-url "https://etherscan.io"}
   :graphql {:schema graphql-schema
             :url "http://memefactory.io:6300/graphql"}
   :ipfs {:host "http://memefactory.io:5001" :endpoint "/api/v0"}})

(def config-map
  (condp = (get-environment)
    "prod" production-config
    "qa"   qa-config
    "dev"  development-config))

(defn start []
  (re-frame/dispatch [::load-memefactory-db-params (-> config-map :graphql :url)]))

(re-frame/reg-event-fx
 ::load-memefactory-db-params
 (fn [cofx [_ graphql-url]]
   (js/console.log "Loading initial params from " graphql-url)
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
                         (into {}))]
     (assoc db ::memefactory-db-params params-map))))

(re-frame/reg-sub
 ::memefactory-db-params
 (fn [db _]
   (::memefactory-db-params db)))

(defstate config
  :start (start)
  :stop ::stopped)
