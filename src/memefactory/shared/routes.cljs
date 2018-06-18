(ns memefactory.shared.routes)

(def routes [["/" :route/home]
             ["/marketplace/index" :route.marketplace/index]
             ["/dankregistry/index" :route.dank-registry/index]
             ["/dankregistry/submit" :route.dank-registry/submit]
             ["/dankregistry/:registry-address/vote" :route.dank-registry/vote]
             ["/dankregistry/:registry-address/challenge" :route.dank-registry/challenge]
             ["/dankregistry/browse" :route.dank-registry/browse]
             ["/memefolio/index" :route.memefolio/index]])
