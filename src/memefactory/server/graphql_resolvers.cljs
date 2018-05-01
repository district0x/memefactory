(ns memefactory.server.graphql-resolvers
  (:require [memefactory.shared.graphql-mock-resolvers :refer [graphql-mock-resolvers]]))

(def graphql-resolvers
  {:meme (fn [args context document])
   :search-memes (fn [args context document])
   :search-meme-tokens (fn [args context document])
   :meme-auction (fn [args context document])
   :search-meme-auctions (fn [args context document])
   :search-tags (fn [args context document])
   :param-change (fn [args context document])
   :search-param-changes (fn [args context document])
   :user (fn [args context document])
   :search-users (fn [args context document])
   :param (fn [args context document])
   :params (fn [args context document])})