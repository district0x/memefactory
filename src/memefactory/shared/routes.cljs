(ns memefactory.shared.routes)

(def routes [["/" :route/home]
             ["/marketplace/index" :route.marketplace/index]
             ["/dankregistry/index" :route.dankregistry/index]
             ["/dankregistry/submit" :route.dankregistry/submit]
             ["/dankregistry/:registry-address/vote" :route.dankregistry/vote]
             ["/dankregistry/:registry-address/challenge" :route.dankregistry/challenge]
             ["/dankregistry/browse" :route.dankregistry/browse]
             ["/memefolio/index" :route.memefolio/index]])
