(defproject memefactory "1.0.0"
  :description "Create and trade provably rare digital assets on the Ethereum blockchain"
  :url "https://github.com/district0x/memefactory"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.8.0"]]

  :plugins [[lein-auto "0.1.2"]
            [lein-cljsbuild "1.1.7"]
            [lein-figwheel "0.5.14"]
            [lein-shell "0.5.0"]
            [lein-doo "0.1.8"]
            [lein-npm "0.6.2"]
            [lein-pdo "0.1.1"]]

  :auto {"compile-solidity" {:file-pattern #"\.(sol)$"
                             :paths ["resources/public/contracts/src"]}}

  :aliases {"compile-solidity" ["shell" "./compile-solidity.sh"]})
