(ns memefactory.shared.graphql-schema)

(def graphql-schema "
  type Query {
    meme(address: ID!): Meme
    search: [Meme]
  }

  type Meme {
    title: String
  }

  ")