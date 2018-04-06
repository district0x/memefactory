(ns memefactory.tests.runner
  (:require
    [cljs.nodejs :as nodejs]
    [cljs.pprint]
    [cljs.test :refer [run-tests]]
    [memefactory.tests.smart-contracts.deployment-tests]
    [memefactory.tests.smart-contracts.meme-tests]))

(nodejs/enable-util-print!)

(set! (.-error js/console) (fn [x] (.log js/console x)))

(defn -main [& _]
  (run-tests 'memefactory.tests.smart-contracts.deployment-tests
             'memefactory.tests.smart-contracts.meme-tests))

(set! *main-cli-fn* -main)