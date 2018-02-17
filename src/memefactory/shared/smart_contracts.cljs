(ns memefactory.shared.smart-contracts)

(def smart-contracts
  {:MFM {:name "MemeFactoryMoola"
         :address "0x0000000000000000000000000000000000000000"}
   :meme {:name "Meme"
          :address "0x0000000000000000000000000000000000000000"}
   :meme-factory {:name "MemeFactory"
                  :address "0x0000000000000000000000000000000000000000"}
   :meme-token {:name "MemeToken"
                :address "0x0000000000000000000000000000000000000000"}
   :meme-registry {:name "Registry"
                   :address "0x0000000000000000000000000000000000000000"}
   :meme-registry-fwd {:name "MutableForwarder"
                       :address "0x0000000000000000000000000000000000000000"}
   :meme-registry-db {:name "EternalStorage"
                      :address "0x0000000000000000000000000000000000000000"}
   :parameter-change {:name "ParameterChange"
                      :address "0x0000000000000000000000000000000000000000"}
   :parameter-change-factory {:name "ParameterChangeFactory"
                              :address "0x0000000000000000000000000000000000000000"}
   :parameter-registry {:name "ParameterRegistry"
                        :address "0x0000000000000000000000000000000000000000"}
   :parameter-registry-fwd {:name "MutableForwarder"
                            :address "0x0000000000000000000000000000000000000000"}
   :parameter-registry-db {:name "EternalStorage"
                           :address "0x0000000000000000000000000000000000000000"}})
