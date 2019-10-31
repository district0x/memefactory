(ns memefactory.server.dev
  (:require
   [ajax.core :as http]
   [bignumber.core :as bn]
   [camel-snake-kebab.core :as cs :include-macros true]
   [cljs-time.core :as t]
      ;; TODO
   ;; [cljs-web3.core :as web3-core]
   ;; [cljs-web3.eth :as web3-eth]
   ;; [cljs-web3.evm :as web3-evm]
#_   [district.server.web3-watcher]
   [cljs.nodejs :as nodejs]
   [cljs.pprint :as pprint]
   [clojure.pprint :refer [print-table]]
   [clojure.string :as string]
   [district.graphql-utils :as graphql-utils]
   [district.server.config :refer [config]]
   [district.server.db :as db]
   [district.server.graphql :as graphql]
   [district.server.graphql.utils :as utils]
   [district.server.logging :refer [logging]]
   [district.server.middleware.logging :refer [logging-middlewares]]
   [district.server.smart-contracts]
   [district.server.web3 :refer [web3]]
   [district.server.web3-events]
   [district.shared.async-helpers :as async-helpers]
   [goog.date.Date]
   [graphql-query.core :refer [graphql-query]]
   [memefactory.server.constants :as constants]
   [memefactory.server.contract.dank-token :as dank-token]
   [memefactory.server.contract.eternal-db :as eternal-db]
   [memefactory.server.contract.registry-entry :as registry-entry]
   [memefactory.server.conversion-rates]
   [memefactory.server.dank-faucet-monitor]
   [memefactory.server.db :as memefactory-db]
   [memefactory.server.emailer]
   [memefactory.server.generator :as generator]
   [memefactory.server.graphql-resolvers :refer [resolvers-map reg-entry-status-sql-clause]]
   [memefactory.server.ipfs]
   [memefactory.server.pinner]
   [memefactory.server.ranks-cache]
   [memefactory.server.syncer :as syncer]
   [memefactory.server.twitter-bot]
   [memefactory.server.utils :as server-utils]
   [memefactory.shared.graphql-schema :refer [graphql-schema]]
   [memefactory.shared.smart-contracts-dev :as smart-contracts-dev]
   [memefactory.shared.smart-contracts-prod :as smart-contracts-prod]
   [memefactory.shared.smart-contracts-qa :as smart-contracts-qa]
   [memefactory.shared.utils :as shared-utils]
   [mount.core :as mount]
   [cljs-promises.async]
   [taoensso.timbre :as log])
  (:require-macros [memefactory.shared.utils :refer [get-environment]]))

(nodejs/enable-util-print!)

(def child-process (nodejs/require "child_process"))
(def spawn (aget child-process "spawn"))

(def graphql-module (nodejs/require "graphql"))
(def parse-graphql (aget graphql-module "parse"))
(def visit (aget graphql-module "visit"))

(defn on-jsload []
  (graphql/restart {:schema (utils/build-schema graphql-schema
                                                resolvers-map
                                                {:kw->gql-name graphql-utils/kw->gql-name
                                                 :gql-name->kw graphql-utils/gql-name->kw})
                    :field-resolver (utils/build-default-field-resolver graphql-utils/gql-name->kw)}))

(defn redeploy
  "Redeploy smart contracts with truffle"
  []
  (log/warn "Redeploying contracts, please be patient..." ::redeploy)
  (let [child (spawn "truffle migrate --network ganache --f 2 --to 3 --reset" (clj->js {:stdio "inherit" :shell true}))]
    (-> child
        (.on "disconnect" (fn []
                            (log/warn "Parent process has disconnected" ::redeploy)))
        (.on "exit" (fn [code signal]
                      (log/info "Truffle migrate process exited" {:code code
                                                                  :signal signal} ::redeploy)))
        (.on "error" (fn [err]
                       (log/error "Truffle migrate process error" {:error err} ::redeploy)))

        (.on "close" (fn []
                       (log/info "Finished redploying contracts" ::redeploy))))))


;; TODO : generator

(defn generate-data
  "Generate dev data from supplied scenarios.
   Basic usage:
   `(generate-data {:create-meme true
                    :challenge-meme true
                    :commit-votes true
                    :reveal-votes true
                    :claim-vote-rewards true
                    :mint-meme-tokens true
                    :start-auctions true
                    :buy-auctions true})`
   You can also override default options, e.g.:
   `:reveal-votes {:option :vote.option/vote-for
                   :amount 2
                   :salt \"abc\"
                   :from-account 0}`
   or skip a step by passing `false`, e.g:
   `:challenge-meme false`"
  [& [scenario]]
  (let [scenario (or scenario
                     {:create-meme {:image-file "resources/dev/pepe.png"
                                    :title "Pepe"
                                    :total-supply 2
                                    :from-account 0}

                      :challenge-meme {:comment "its too baroque"
                                       :from-account 9}

                      :commit-votes [{:option :vote.option/vote-for
                                      :amount 2
                                      :salt "abc"
                                      :from-account 0}
                                     {:option :vote.option/vote-against
                                      :amount 1
                                      :salt "abc"
                                      :from-account 9}]

                      :reveal-votes true

                      ;; meme is whitelisted in this scenario
                      :claim-challenge-reward false

                      :claim-vote-rewards [{:from-account 0}]

                      :mint-meme-tokens {:amount 2
                                         :from-account 0}

                      :start-auctions {:start-price 0.5
                                       :end-price 0.1
                                       :duration 12000000
                                       :description "buy it"
                                       :from-account 0}

                      :buy-auctions [{:price 0.5
                                      :from-account 3}]})]
    (log/info "Generating data, please be patient..." {:scenario scenario} ::generate-date)
    (generator/generate-memes scenario)))

(defn resync []
  (log/warn "Syncing internal database, please be patient..." ::resync)
  (memefactory-db/clean-db)
  (mount/stop #'district.server.web3-events/web3-events
              #'memefactory.server.syncer/syncer
              #'district.server.smart-contracts/smart-contracts)
  (-> (mount/start #'district.server.web3-events/web3-events
                   #'memefactory.server.syncer/syncer
                   #'district.server.smart-contracts/smart-contracts)
      (log/info "Finished syncing database")))


(def contracts-var
  (condp = (get-environment)
    "prod" #'smart-contracts-prod/smart-contracts
    "qa" #'smart-contracts-qa/smart-contracts
    "qa-dev" #'smart-contracts-qa/smart-contracts
    "dev" #'smart-contracts-dev/smart-contracts))


(defn start []
  (-> (mount/with-args
        {:config {:default {:logging {:level "info"
                                      :console? true}
                            :time-source :blockchain
                            :graphql {:port 6300
                                      :middlewares [logging-middlewares]
                                      :schema (utils/build-schema graphql-schema
                                                                  resolvers-map
                                                                  {:kw->gql-name graphql-utils/kw->gql-name
                                                                   :gql-name->kw graphql-utils/gql-name->kw})
                                      :field-resolver (utils/build-default-field-resolver graphql-utils/gql-name->kw)
                                      :path "/graphql"
                                      :graphiql true}
                            :web3 {:url "http://localhost:8549"}
                            :ipfs {:host "http://127.0.0.1:5001"
                                   :endpoint "/api/v0"
                                   :gateway "http://127.0.0.1:8080/ipfs"}
                            :smart-contracts {:contracts-var contracts-var}
                            :ranks-cache {:ttl (t/in-millis (t/minutes 60))}
                            :ui {:public-key "PLACEHOLDER"
                                 :root-url "http://0.0.0.0:4598/#/"}
                            :twilio-api-key "PLACEHOLDER"
                            :blacklist-file "blacklist.edn"
                            :blacklist-token "PLACEHOLDER"
                            :sigterm {:on-sigterm (fn [args]
                                                    (log/warn "Received SIGTERM signal. Exiting" {:args args})
                                                    (mount/stop)
                                                    (.exit nodejs/process 0))}
                            :emailer {:private-key "PLACEHOLDER"
                                      :api-key "PLACEHOLDER"
                                      :template-id "PLACEHOLDER"
                                      :from "noreply@memefactory.io"
                                      :print-mode? true}
                            :twitter-bot {:consumer-key "PLACEHOLDER"
                                          :consumer-secret "PLACEHOLDER"
                                          :access-token-key "PLACEHOLDER"
                                          :access-token-secret "PLACEHOLDER"
                                          :just-log-tweet? true}
                            :pinner {:disabled? true}
                            :web3-watcher {:interval 3000
                                           :confirmations 3
                                           :on-offline (fn []
                                                         (log/error "Ethereum node went offline, stopping syncing modules" ::web3-watcher)
                                                         (mount/stop #'memefactory.server.db/memefactory-db
                                                                     #'district.server.web3-events/web3-events
                                                                     #'memefactory.server.syncer/syncer
                                                                     #'memefactory.server.pinner/pinner
                                                                     #'memefactory.server.emailer/emailer))
                                           :on-online (fn []
                                                        (log/warn "Ethereum node went online again, starting syncing modules" ::web3-watcher)
                                                        (mount/start #'memefactory.server.db/memefactory-db
                                                                     #'district.server.web3-events/web3-events
                                                                     #'memefactory.server.syncer/syncer
                                                                     #'memefactory.server.pinner/pinner
                                                                     #'memefactory.server.emailer/emailer))}
                            :web3-events {:events constants/web3-events}}}})
      (mount/start)
      (as-> $ (log/warn "Started" {:components $
                                   :config @config}))))

(defn stop []
  (mount/stop))

(defn restart []
  (stop)
  (start))

(defn -main [& args]

  (async-helpers/extend-promises-as-channels!)

  (.on js/process "unhandledRejection"
       (fn [reason p] (log/error "Unhandled promise rejection " {:reason reason})))

  (when-not (= (last args) "--nostart")
    (log/debug "Mounting... (Pass the argument --nostart to prevent mounting on start)")
    (start)))

(set! *main-cli-fn* -main)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Some useful repl tools ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#_(defn increase-time [seconds]
    (web3-evm/increase-time! @web3 [seconds])
    (web3-evm/mine! @web3))

(defn select
  "Usage: (select [:*] :from [:memes])"
  [& [select-fields & r]]
  (-> (db/all (->> (partition 2 r)
                   (map vec)
                   (into {:select select-fields})))
      (print-table)))

(defn print-db
  "(print-db) prints all db tables to the repl
   (print-db :users) prints only users table"
  ([] (print-db nil))
  ([table]
   (let [all-tables (if table
                      [(name table)]
                      (->> (db/all {:select [:name] :from [:sqlite-master] :where [:= :type "table"]})
                           (map :name)))]
     (doseq [t all-tables]
       (println "#######" (string/upper-case t) "#######")
       (select [:*] :from [(keyword t)])
       #_(println "\n\n")))))

#_(defn print-statuses []
  (web3-evm/mine! @web3) ;; We need to mine a block so time make sense
  (->> (db/all {:select [:v.* :re.*]
                :from [[:reg-entries :re]]
                :left-join [[:votes :v] [:= :re.reg-entry/address :v.reg-entry/address]]})
       (group-by :reg-entry/address)
       (map (fn [[address [r :as votes]]]
              {:address address
               :server-status (name (shared-utils/reg-entry-status (server-utils/now-in-seconds) r))
               :query-status (-> (db/get {:select [[(reg-entry-status-sql-clause (server-utils/now-in-seconds)) :status]]
                                          :from  [[:reg-entries :re]]
                                          :where [:= :re.reg-entry/address address]})
                                 :status
                                 graphql-utils/gql-name->kw
                                 name)
               :v+ (:challenge/votes-for r)
               :v- (:challenge/votes-against r)
               :v? (count (filter #(and (pos? (:vote/amount %))
                                        (or (zero? (:vote/revealed-on %))
                                            (nil? (:vote/revealed-on %)))) votes))}))
       #_print-table))

#_(defn increase-time-to-next-period [re-address]
  (let [now (server-utils/now-in-seconds)
        entry (db/get {:select [:*]
                       :from [:reg-entries]
                       :where [:= :reg-entry/address re-address]})
        current-status (shared-utils/reg-entry-status now entry)
        time-to-next (case current-status
                       :reg-entry.status/challenge-period (- (:reg-entry/challenge-period-end entry) now)
                       :reg-entry.status/commit-period    (- (:challenge/commit-period-end entry) now)
                       :reg-entry.status/reveal-period    (- (:challenge/reveal-period-end entry) now)
                       :reg-entry.status/whitelisted      (println "Not moving for whitelisted")
                       :reg-entry.status/blacklisted      (println "Not moving for blacklisted"))]
    (println "Increasing time by " time-to-next)
    (increase-time time-to-next)
    (when (#{:reg-entry.status/challenge-period :reg-entry.status/reveal-period} current-status)
      (syncer/meme-number-assigner re-address))))

#_(defn print-balances []
    (->> (web3-eth/accounts @web3)
         (map (fn [account]
                {:account account
                 :dank (bn/number (dank-token/balance-of account))
                 :eth (bn/number (web3-eth/get-balance @web3 account))}))
         #_print-table))

#_(defn print-params []
  (let [param-keys [:max-total-supply :deposit :challenge-period-duration
                    :commit-period-duration :reveal-period-duration :max-auction-duration
                    :vote-quorum :challenge-dispensation]]
    (->> (eternal-db/get-uint-values :meme-registry-db param-keys)
         (map bn/number)
         (zipmap param-keys)
         #_pprint/pprint)))

(defn print-ranks-cache []
  (pprint/pprint @@memefactory.server.ranks-cache/ranks-cache))

#_(defn transfer-dank [account dank-amount]
    (let [accounts (web3-eth/accounts @web3)]
      (dank-token/transfer {:to account :amount (web3-core/to-wei dank-amount :ether)}
                           ;; this is the deployer of dank-token so it owns the initial amount
                           {:from (first accounts)})))
