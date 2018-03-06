(ns memefactory.server.graphql-root)

(def graphql-root
  {:search (constantly (clj->js [{:title "MyMeeeeaaaaa"}]))})
