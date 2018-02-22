(defproject memefactory "1.0.0"
  :description "Create and trade provably rare digital assets on the Ethereum blockchain"
  :url "https://github.com/district0x/memefactory"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[camel-snake-kebab "0.4.0"]
                 [cljs-web3 "0.19.0-0-10"]
                 [com.andrewmcveigh/cljs-time "0.5.2"]
                 [com.taoensso/encore "2.92.0"]
                 [com.taoensso/timbre "4.10.0"]
                 [district0x/bignumber "1.0.1"]
                 [district0x/cljs-solidity-sha3 "1.0.0"]
                 [district0x/district-cljs-utils "1.0.0"]
                 [district0x/district-encryption "1.0.0"]
                 [district0x/district-sendgrid "1.0.0"]
                 [district0x/district-server-config "1.0.1"]
                 [district0x/district-server-db "1.0.1"]
                 [district0x/district-server-endpoints "1.0.2"]
                 [district0x/district-server-logging "1.0.1"]
                 [district0x/district-server-smart-contracts "1.0.5"]
                 [district0x/district-server-web3 "1.0.1"]
                 [district0x/district-server-web3-watcher "1.0.2"]
                 [medley "1.0.0"]
                 [mount "0.1.12"]
                 [org.clojure/clojurescript "1.9.946"]
                 [print-foo-cljs "2.0.3"]]

  :plugins [[lein-auto "0.1.2"]
            [lein-cljsbuild "1.1.7"]
            [lein-figwheel "0.5.14"]
            [lein-shell "0.5.0"]
            [lein-doo "0.1.8"]
            [lein-npm "0.6.2"]
            [lein-pdo "0.1.1"]]

  :npm {:dependencies [[semantic-ui "2.2.14"]
                       [solc "0.4.20"]
                       [source-map-support "0.5.3"]
                       [ws "4.0.0"]]}

  :source-paths ["src" "test"]

  :figwheel {:server-port 4598
             :css-dirs ["resources/public/css"]}

  :auto {"compile-solidity" {:file-pattern #"\.(sol)$"
                             :paths ["resources/public/contracts/src"]}}

  :aliases {"compile-solidity" ["shell" "./compile-solidity.sh"]
            "clean-prod-server" ["shell" "rm" "-rf" "server"]
            "watch-css" ["shell" "./semantic.sh" "watch"]
            "build-css" ["shell" "./semantic.sh" "build-css"]
            "build-prod-server" ["do" ["clean-prod-server"] ["cljsbuild" "once" "server"]]
            "build-prod-ui" ["do" ["clean"] ["cljsbuild" "once" "ui"]]
            "build-prod" ["pdo" ["build-prod-server"] ["build-prod-ui"] ["build-css"]]}

  :profiles {:dev {:dependencies [[org.clojure/clojure "1.9.0"]
                                  [binaryage/devtools "0.9.9"]
                                  [com.cemerick/piggieback "0.2.2"]
                                  [figwheel-sidecar "0.5.14" :exclusions [org.clojure/core.async]]
                                  [org.clojure/tools.reader "1.2.1"]]
                   :source-paths ["dev" "src"]
                   :resource-paths ["resources"]}}

  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src/memefactory/ui" "src/memefactory/shared"]
                        :figwheel {:on-jsload "memefactory.ui.core/mount-root"}
                        :compiler {:main "memefactory.ui.core"
                                   :output-to "resources/public/js/compiled/app.js"
                                   :output-dir "resources/public/js/compiled/out"
                                   :asset-path "js/compiled/out"
                                   :source-map-timestamp true
                                   :preloads [print.foo.preloads.devtools]
                                   :closure-defines {goog.DEBUG true}
                                   :external-config {:devtools/config {:features-to-install :all}}}}
                       {:id "dev-server"
                        :source-paths ["src/memefactory/server" "src/memefactory/shared"]
                        :figwheel {:on-jsload "memefactory.server.dev/on-jsload"}
                        :compiler {:main "memefactory.server.dev"
                                   :output-to "dev-server/memefactory.js"
                                   :output-dir "dev-server"
                                   :target :nodejs
                                   :optimizations :none
                                   :closure-defines {goog.DEBUG true}
                                   :source-map true}}
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
                        :source-paths ["src/memefactory/server" "src/memefactory/shared" "test/server"]
                        :figwheel true
                        :compiler {:main "server.run-tests"
                                   :output-to "server-tests/server-tests.js",
                                   :output-dir "server-tests",
                                   :target :nodejs,
                                   :optimizations :none,
                                   :verbose false
                                   :source-map true}}]})
