'use strict';

const MEMEFACTORY_ENV = process.env.MEMEFACTORY_ENV || "dev";
const HDWalletProvider = require("truffle-hdwallet-provider");
require('dotenv').config()  // Store environment-specific variable from '.env' to process.env

const smartContractsPaths = {
  "dev" : '/src/memefactory/shared/smart_contracts_dev.cljs',
  "qa" : '/src/memefactory/shared/smart_contracts_qa.cljs',
  "prod" :'/src/memefactory/shared/smart_contracts_prod.cljs'
};

let parameters = {
  "qa" : {
    memeRegistryDb : {challengePeriodDuration : 600, // seconds (10 minutes)
                      commitPeriodDuration : 600, // seconds
                      revealPeriodDuration : 600, // seconds
                      deposit : 1e18, // 1e18 = 1 DANK
                      challengeDispensation : 50, // percent
                      maxTotalSupply : 10, // int
                      maxAuctionDuration : 1.21e6 // seconds
                     },
    paramChangeRegistryDb : {challengePeriodDuration : 600, // seconds
                             commitPeriodDuration : 600, // seconds
                             revealPeriodDuration : 600, // seconds
                             deposit : 1e18, // 1e18 = 1 DANK
                             challengeDispensation : 50 // percent
                            },
    dankFaucet : {dank : 5000000e18, // how much DANK contract holds, 1e18 = 1 DANK
                  eth : 0.1e18, // ETH, 1e18 = 1ETH
                  allotment : 2000e18  // how much DANK faucet sends, 1e18 = 1 DANK
                 }
  },
  "prod" : {
    memeRegistryDb : {challengePeriodDuration : 86400, // seconds (24h)
                      commitPeriodDuration : 86400, // seconds
                      revealPeriodDuration : 86400, // seconds
                      deposit : 100e18, // 1e18 = 1 DANK
                      challengeDispensation : 50, // percent
                      maxTotalSupply : 100, // int
                      maxAuctionDuration : (30 * 86400) // seconds
                     },
    paramChangeRegistryDb : {challengePeriodDuration : 86400, // seconds
                             commitPeriodDuration : 86400, // seconds
                             revealPeriodDuration : 86400, // seconds
                             deposit : 1000000000e18, // 1e18 = 1 DANK
                             challengeDispensation : 50 // percent
                            },
    dankFaucet : {dank : 5000000e18, // how much DANK contract holds, 1e18 = 1 DANK
                  eth : 0.2e18, // ETH, 1e18 = 1ETH
                  allotment : 2000e18  // how much DANK faucet sends, 1e18 = 1 DANK
                 }
  }
};

parameters.dev = parameters.qa;

module.exports = {
  env: MEMEFACTORY_ENV,
  smart_contracts_path: __dirname + smartContractsPaths [MEMEFACTORY_ENV],
  contracts_build_directory: __dirname + '/resources/public/contracts/build/',
  parameters : parameters [MEMEFACTORY_ENV],
  networks: {
    "ganache": {
      host: 'localhost',
      port: 8545,
      gas: 1e6, // gas limit
      gasPrice: 20e9, // 20 gwei, default for ganache
      network_id: '*'
    },
    "infura-ropsten": {
      provider: () => new HDWalletProvider(process.env.ROPSTEN_PRIV_KEY, "https://ropsten.infura.io/v3/" + process.env.INFURA_API_KEY),
      network_id: 1,
      gas: 1e6,
      gasPrice: 20e9,
      skipDryRun: true
    },
    "infura-mainnet": {
      provider: () => new HDWalletProvider(process.env.MAINNET_PRIV_KEY, "https://mainnet.infura.io/v3/" + process.env.INFURA_API_KEY),
      network_id: 2,
      gas: 1e6,
      gasPrice: 40e9,
      skipDryRun: true
    }
  }
};
