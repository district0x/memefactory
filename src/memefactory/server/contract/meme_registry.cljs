(ns memefactory.server.contract.meme-registry
  (:require [memefactory.server.contract.registry :as registry]))

#_(def registry-entry-event (partial registry/registry-entry-event [:meme-registry :meme-registry-fwd]))
#_(def registry-entry-event-in-tx (partial registry/registry-entry-event-in-tx [:meme-registry :meme-registry-fwd]))
