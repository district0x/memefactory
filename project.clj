(defproject memefactory "1.0.0"
  :description "Create and trade provably rare digital assets on the Ethereum blockchain"
  :url "https://github.com/district0x/memefactory"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[camel-snake-kebab "0.4.0"]
                 [cljs-web3 "0.19.0-0-10"]
                 [cljsjs/react-infinite "0.13.0-0"]
                 [com.andrewmcveigh/cljs-time "0.5.2"]
                 [com.taoensso/encore "2.92.0"]
                 [com.taoensso/timbre "4.10.0"]
                 [day8.re-frame/re-frame-10x "0.3.1"]
                 [district0x/bignumber "1.0.3"]
                 [district0x/cljs-solidity-sha3 "1.0.0"]
                 [district0x/district-cljs-utils "1.0.3"]
                 [district0x/district-encryption "1.0.0"]
                 [district0x/district-format "1.0.0"]
                 [district0x/district-graphql-utils "1.0.5"]
                 [district0x/district-sendgrid "1.0.0"]
                 [district0x/district-server-config "1.0.1"]
                 [district0x/district-server-db "1.0.2"]
                 [district0x/district-server-endpoints "1.0.3"]
                 [district0x/district-server-graphql "1.0.15"]
                 [district0x/district-server-logging "1.0.1"]
                 [district0x/district-server-middleware-logging "1.0.0"]
                 [district0x/district-server-smart-contracts "1.0.8"]
                 [district0x/district-server-web3 "1.0.1"]
                 [district0x/district-server-web3-watcher "1.0.2"]
                 [district0x/district-ui-component-active-account "1.0.0"]
                 [district0x/district-ui-component-active-account-balance "1.0.0"]
                 [district0x/district-ui-component-notification "1.0.0"]
                 [district0x/district-ui-component-tx-button "1.0.0"]
                 [district0x/district-ui-component-input "1.0.0"]
                 [district0x/district-ui-graphql "1.0.3"]
                 [district0x/district-ui-logging "1.0.1"]
                 [district0x/district-ui-notification "1.0.1"]
                 [district0x/district-ui-now "1.0.2"]
                 [district0x/district-ui-server-config "1.0.0"]
                 [district0x/district-ui-web3-sync-now "1.0.3"]
                 [district0x/district-ui-reagent-render "1.0.1"]
                 [district0x/district-ui-router "1.0.3"]
                 [district0x/district-ui-router-google-analytics "1.0.0"]
                 [district0x/district-ui-smart-contracts "1.0.5"]
                 [district0x/district-ui-web3 "1.0.1"]
                 [district0x/district-ui-web3-account-balances "1.0.2"]
                 [district0x/district-ui-web3-accounts "1.0.5"]
                 [district0x/district-ui-web3-balances "1.0.2"]
                 [district0x/district-ui-web3-tx "1.0.8"]
                 [district0x/district-ui-web3-tx-id "1.0.1"]
                 [district0x/district-ui-web3-tx-log "1.0.2"]
                 [district0x/district-ui-window-size "1.0.1"]
                 [district0x/district-web3-utils "1.0.2"]
                 [district0x/district-format "1.0.0"]
                 [district0x/district-time "1.0.0"]
                 [district0x/district-ui-component-form "0.1.0-SNAPSHOT"]
                 [medley "1.0.0"]
                 [mount "0.1.12"]
                 [org.clojure/clojurescript "1.10.238"]
                 [print-foo-cljs "2.0.3"]
                 [re-frame "0.10.5"]
                 [garden "1.3.5"]]

  :exclusions [express-graphql]

  :plugins [[lein-auto "0.1.2"]
            [lein-cljsbuild "1.1.7"]
            [lein-figwheel "0.5.16"]
            [lein-shell "0.5.0"]
            [lein-solc "1.0.0"]
            [lein-doo "0.1.8"]
            [lein-npm "0.6.2"]
            [lein-pdo "0.1.1"]
            [lein-garden "0.3.0"]]

  :npm {:dependencies [#_[semantic-ui "2.2.14"]
                       ;; needed until v0.6.13 is officially released
                       [express-graphql "./resources/libs/express-graphql-0.6.13.tgz"]
                       [graphql-tools "3.0.1"]
                       [graphql "0.13.1"]
                       [express "4.15.3"]
                       [cors "2.8.4"]
                       [graphql-fields "1.0.2"]
                       [solc "0.4.20"]
                       [source-map-support "0.5.3"]
                       [ws "4.0.0"]]}

  :solc {:src-path "resources/public/contracts/src"
         :build-path "resources/public/contracts/build"
         :solc-err-only true
         :wc true
         :contracts ["DankToken.sol"
                     "MemeFactory.sol"
                     "MemeAuctionFactory.sol"
                     "ParamChangeFactory.sol"
                     "ParamChangeRegistry.sol"]}

  :source-paths ["src" "test"]

  :figwheel {:server-port 4598
             :css-dirs ["resources/public/css"]
             :repl-eval-timeout 30000}

  :aliases {"clean-prod-server" ["shell" "rm" "-rf" "server"]
            "watch-semantic" ["shell" "./semantic.sh" "watch"]
            "build-semantic" ["shell" "./semantic.sh" "build-css"]
            "build-prod-server" ["do" ["clean-prod-server"] ["cljsbuild" "once" "server"]]
            "build-prod-ui" ["do" ["clean"] ["cljsbuild" "once" "ui"]]
            "build-prod" ["pdo" ["build-prod-server"] ["build-prod-ui"] ["build-css"]]
            "build-tests" ["cljsbuild" "once" "server-tests"]
            "test" ["do" ["build-tests"] ["shell" "node" "memefactory-tests/memefactory-server-tests.js"]]
            "test-doo" ["doo" "node" "server-tests"]}

  :profiles {:dev {:dependencies [[org.clojure/clojure "1.9.0"]
                                  [binaryage/devtools "0.9.9"]
                                  [com.cemerick/piggieback "0.2.2"]
                                  [figwheel-sidecar "0.5.14" :exclusions [org.clojure/core.async]]
                                  [org.clojure/tools.reader "1.2.1"]]
                   :source-paths ["dev" "src"]
                   :resource-paths ["resources"]}}

  :garden {:builds [{:id "screen"
                     :source-paths ["src"]
                     :stylesheet memefactory.styles.core/main
                     ;; Compiler flags passed to `garden.core/css`:
                     :compiler {;; Where to save the file:
                                :output-to "resources/public/css/main.css"
                                ;; Compress the output?
                                :pretty-print? false}}]}

  :cljsbuild {:builds [{:id "dev-server"
                        :source-paths ["src/memefactory/server" "src/memefactory/shared" "test/memefactory"]
                        :figwheel {:on-jsload "memefactory.server.dev/on-jsload"}
                        :compiler {:main "memefactory.server.dev"
                                   :output-to "dev-server/memefactory.js"
                                   :output-dir "dev-server"
                                   :target :nodejs
                                   :optimizations :none
                                   :closure-defines {goog.DEBUG true}
                                   :source-map true}}
                       {:id "dev"
                        :source-paths ["src/memefactory/ui" "src/memefactory/shared"]
                        :figwheel {:on-jsload "district.ui.reagent-render/rerender"}
                        :compiler {:main "memefactory.ui.core"
                                   :output-to "resources/public/js/compiled/app.js"
                                   :output-dir "resources/public/js/compiled/out"
                                   :asset-path "js/compiled/out"
                                   :source-map-timestamp true
                                   :preloads [print.foo.preloads.devtools
                                              day8.re-frame-10x.preload]
                                   :closure-defines {goog.DEBUG true
                                                     "re_frame.trace.trace_enabled_QMARK_" true}
                                   :external-config {:devtools/config {:features-to-install :all}}}}
                       {:id "server"
                        :source-paths ["src"]
                        :compiler {:main "memefactory.server.core"
                                   :output-to "server/memefactory.js"
                                   :output-dir "server"
                                   :target :nodejs
                                   :optimizations :simple
                                   :source-map "server/memefactory.js.map"
                                   :closure-defines {goog.DEBUG false}
                                   :pretty-print false}}
                       {:id "ui"
                        :source-paths ["src"]
                        :compiler {:main "memefactory.ui.core"
                                   :output-to "resources/public/js/compiled/app.js"
                                   :optimizations :advanced
                                   :closure-defines {goog.DEBUG false}
                                   :pretty-print false
                                   :pseudo-names false}}
                       {:id "server-tests"
                        :source-paths ["src/memefactory/server" "src/memefactory/shared" "test/memefactory"]
                        :figwheel {:on-jsload "memefactory.tests.runner/on-jsload"}
                        :compiler {:main "memefactory.tests.runner"
                                   :output-to "memefactory-tests/memefactory-server-tests.js",
                                   :output-dir "memefactory-tests",
                                   :target :nodejs,
                                   :optimizations :none,
                                   :verbose false
                                   ;;:closure-defines {goog.DEBUG true}
                                   :source-map true}}]})
