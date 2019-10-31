(defproject memefactory "1.0.0"
  :description "Create and trade provably rare digital assets on the Ethereum blockchain"
  :url "https://github.com/district0x/memefactory"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[akiroz.re-frame/storage "0.1.2"]
                 [camel-snake-kebab "0.4.0"]
                 [cljs-node-io "1.1.2"]
                 [cljsjs/buffer "5.1.0-1"]
                 [cljsjs/d3 "4.12.0-0"]
                 [cljsjs/filesaverjs "1.3.3-0"]
                 [cljsjs/jquery "3.2.1-0"]
                 [cljsjs/react "16.4.1-0"]
                 [cljsjs/react-dom "16.4.1-0"]
                 [cljsjs/react-infinite "0.13.0-0"]
                 [com.andrewmcveigh/cljs-time "0.5.2"]
                 [com.taoensso/encore "2.92.0"]
                 [com.taoensso/timbre "4.10.0"]
                 [day8.re-frame/http-fx "0.1.6"]
                 [district0x/async-helpers "0.1.3"]
                 [district0x/bignumber "1.0.3"]
                 [district0x/cljs-ipfs-http-client "1.0.5"]
                 [district0x/district-cljs-utils "1.0.4"]
                 [district0x/district-encryption "1.0.1"]
                 [district0x/district-format "1.0.6"]
                 [district0x/district-graphql-utils "1.0.8"]
                 [district0x/district-sendgrid "1.0.1"]
                 [district0x/district-server-config "1.0.1"]
                 [district0x/district-server-db "1.0.4"]
                 [district0x/district-server-graphql "1.0.16"]
                 [district0x/district-server-logging "1.0.5"]
                 [district0x/district-server-middleware-logging "1.0.0"]

                 ;; TODO : update to new releases
                 [cljs-web3-next "0.0.10-SNAPSHOT"]
                 ;; [cljs-web3 "0.19.0-0-10"]
                 ;; [district0x/district-server-smart-contracts "1.0.17"]
                 ;; [district0x/district-server-web3 "1.0.1"]
                 ;; [district0x/district-server-web3-events "1.0.4"]
                 ;; [district0x/district-server-web3-watcher "1.0.3"]

                 [district0x/district-time "1.0.1"]
                 [district0x/district-ui-component-active-account "1.0.1"]
                 [district0x/district-ui-component-active-account-balance "1.0.1"]
                 [district0x/district-ui-component-form "0.2.11"]
                 [district0x/district-ui-component-meta-tags "1.0.0"]
                 [district0x/district-ui-component-notification "1.0.0"]
                 [district0x/district-ui-component-tx-button "1.0.0"]
                 [district0x/district-ui-graphql "1.0.9"]
                 [district0x/district-ui-ipfs "1.0.1"]
                 [district0x/district-ui-logging "1.1.0"]
                 [district0x/district-ui-mobile "1.0.0"]
                 [district0x/district-ui-notification "1.0.1"]
                 [district0x/district-ui-now "1.0.2"]
                 [district0x/district-ui-reagent-render "1.0.1"]
                 [district0x/district-ui-router "1.0.5"]
                 [district0x/district-ui-router-google-analytics "1.0.1"]
                 [district0x/district-ui-smart-contracts "1.0.8"]
                 [district0x/district-ui-web3 "1.3.2"]
                 [district0x/district-ui-web3-account-balances "1.0.2"]
                 [district0x/district-ui-web3-accounts "1.0.6"]
                 [district0x/district-ui-web3-balances "1.0.2"]
                 [district0x/district-ui-web3-sync-now "1.0.3-2"]
                 [district0x/district-ui-web3-tx "1.0.11"]
                 [district0x/district-ui-web3-tx-id "1.0.1"]
                 [district0x/district-ui-web3-tx-log "1.0.11"]
                 [district0x/district-ui-window-size "1.0.1"]
                 [district0x/error-handling "1.0.4"]
                 [district0x/re-frame-ipfs-fx "1.1.1"]
                 [funcool/bide "1.6.1-SNAPSHOT"] ;; version with fix for duplicated query params
                 [garden "1.3.5"]
                 [medley "1.0.0"]
                 [mount "0.1.16"]
                 [org.clojars.mmb90/cljs-cache "0.1.4"]
                 [org.clojure/clojurescript "1.10.439"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [print-foo-cljs "2.0.3"]
                 [re-frame "0.10.5"]
                 [reagent "0.8.1"]

                 ;; only for tests
                 [org.clojure/core.async "0.4.490"]
                 [jamesmacaulay/cljs-promises "0.1.0"]]

  :exclusions [funcool/bide
               express-graphql
               cljsjs/react-with-addons
               ;; before ui is migrated
               cljs-web3]

  :plugins [[lein-auto "0.1.2"]
            [lein-cljsbuild "1.1.7"]
            [lein-figwheel "0.5.18"]
            [lein-shell "0.5.0"]
            [lein-doo "0.1.8"]
            [lein-npm "0.6.2"]
            [lein-pdo "0.1.1"]
            [lein-garden "0.3.0"]]

  :npm {:dependencies [["@sentry/node" "4.2.1"]
                       [better-sqlite3 "5.4.0"]
                       [chalk "2.3.0"]
                       [cors "2.8.4"]
                       [eccjs "0.3.1"]
                       [express "4.15.3"]
                       ;; needed until v0.6.13 is officially released
                       [express-graphql "./resources/libs/express-graphql-0.6.13.tgz"]
                       [graphql "0.13.1"]
                       [graphql-fields "1.0.2"]
                       [graphql-tools "3.0.1"]
                       [is-ipfs "0.4.8"]
                       [source-map-support "0.5.3"]
                       [ws "4.0.0"]
                       [request-promise "4.2.2"]
                       ;; this isn't required directly by memefactory but  0.6.1 is broken and
                       ;; district0x/district-server-web3 needs [ganache-core "2.0.2"]   who also needs "ethereumjs-wallet": "~0.6.0"
                       ;; https://github.com/ethereumjs/ethereumjs-wallet/issues/64
                       ;; [ethereumjs-wallet "0.6.0"]
                       ;; truffle script deps
                       [jsedn "0.4.1"]
                       [web3-utils "1.0.0-beta.55"]
                       ;; for twitter bot
                       [twitter "1.7.1"]
                       [tar-fs "2.0.0"]
                       ;; For deploying to infura
                       [truffle-hdwallet-provider "1.0.12"]
                       [dotenv "8.0.0"]
                       ;; before its in cljsjs
                       [web3 "1.2.0"]
                       ]}

  :source-paths ["src" "test"]

  :figwheel {:server-port 4598
             :css-dirs ["resources/public/css"]
             :repl-eval-timeout 120000}

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "resources/public/contracts/build" "resources/public/css/main.css" "target" "server" "dev-server" "memefactory-tests"]

  :aliases {"clean-prod-server" ["shell" "rm" "-rf" "server"]
            "clean-prod-pinner" ["shell" "rm" "-rf" "pinner"]
            "watch-semantic" ["shell" "./semantic.sh" "watch"]
            "build-semantic" ["shell" "./semantic.sh" "build-css"]
            "build-prod-server" ["do" ["clean-prod-server"] ["cljsbuild" "once" "server"]]
            "build-prod-ui" ["do" ["clean"] ["cljsbuild" "once" "ui"]]
            "build-prod" ["pdo" ["build-prod-server"] ["build-prod-ui"] ["build-css"] ["build-prod-pinner"]]
            "build-prod-pinner" ["do" ["clean-prod-pinner"] ["cljsbuild" "once" "pinner"]]
            "build-tests" ["cljsbuild" "once" "server-tests"]
            "test" ["doo" "node" "server-tests"]
            "test-doo" ["doo" "node" "server-tests"]}

  :profiles {:dev {:dependencies [[org.clojure/clojure "1.9.0"]
                                  [binaryage/devtools "0.9.10"]
                                  [cider/piggieback "0.4.0"]
                                  [figwheel-sidecar "0.5.18"]
                                  [org.clojure/tools.reader "1.3.0"]
                                  [re-frisk "0.5.3"]
                                  [lein-doo "0.1.8"]]
                   :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}
                   :source-paths ["dev" "src"]
                   :resource-paths ["resources"]}
             ;; before ui is moved web3 v1.0
             :ui-deps {:dependencies [;; dev deps
                                      [org.clojure/clojure "1.9.0"]
                                      [binaryage/devtools "0.9.10"]
                                      [cider/piggieback "0.4.0"]
                                      [figwheel-sidecar "0.5.18"]
                                      [org.clojure/tools.reader "1.3.0"]
                                      [re-frisk "0.5.3"]
                                      [lein-doo "0.1.8"]
                                      ;; ui-deps
                                      [cljs-web3 "0.19.0-0-10"]
                                      ;; this is now cljs-web3.utils/solidity-sha3
                                      [district0x/cljs-solidity-sha3 "1.0.0"]
                                      ;; this is now cljs-web3.helpers
                                      [district0x/district-web3-utils "1.0.3"]
                                      ;; these functions should just be a part of cljs-web3.helpers
                                      [district0x/district-parsers "1.0.0"]]}}

  :garden {:builds [{:id "screen"
                     :source-paths ["src"]
                     :stylesheet memefactory.styles.core/main
                     ;; Compiler flags passed to `garden.core/css`:
                     :compiler {;; Where to save the file:
                                :output-to "resources/public/css/main.css"
                                ;; Compress the output?
                                :pretty-print? false}}]}

  :cljsbuild {:builds [{:id "dev-server"
                        :source-paths ["src/memefactory/server" "src/memefactory/shared"  "src/district"]
                        :figwheel {:on-jsload "memefactory.server.dev/on-jsload"}
                        :compiler {:main "memefactory.server.dev"
                                   :output-to "dev-server/memefactory.js"
                                   :output-dir "dev-server"
                                   :target :nodejs
                                   :optimizations :none
                                   :source-map true}}

                       {:id "dev-ui"
                        :source-paths ["src/memefactory/ui" "src/memefactory/shared"]
                        :figwheel {:on-jsload "district.ui.reagent-render/rerender"}
                        :compiler {:main "memefactory.ui.core"
                                   :output-to "resources/public/js/compiled/app.js"
                                   :output-dir "resources/public/js/compiled/out"
                                   :asset-path "js/compiled/out"
                                   :source-map-timestamp true
                                   :preloads [print.foo.preloads.devtools
                                              re-frisk.preload]
                                   :external-config {:devtools/config {:features-to-install :all}}}}
                       {:id "server"
                        :source-paths ["src"]
                        :compiler {:main "memefactory.server.core"
                                   :output-to "server/memefactory.js"
                                   :output-dir "server"
                                   :target :nodejs
                                   :optimizations :simple
                                   :source-map "server/memefactory.js.map"
                                   :pretty-print false}}
                       {:id "ui"
                        :source-paths ["src"]
                        :compiler {:main "memefactory.ui.core"
                                   :output-to "resources/public/js/compiled/app.js"
                                   :optimizations :advanced
                                   :pretty-print true
                                   :pseudo-names true}}
                       {:id "server-tests"
                        :source-paths ["src/memefactory/server" "src/memefactory/shared" "test/memefactory"]
                        :figwheel {:on-jsload "memefactory.tests.runner/on-jsload"}
                        :compiler {:main "memefactory.tests.runner"
                                   :output-to "memefactory-tests/memefactory-server-tests.js",
                                   :output-dir "memefactory-tests",
                                   :target :nodejs,
                                   :optimizations :none,
                                   :verbose false
                                   :source-map true}}]})
