'use strict';

const MEMEFACTORY_ENV = process.env.MEMEFACTORY_ENV || "dev";
const HDWalletProvider = require("@truffle/hdwallet-provider");
require('dotenv').config()  // Store environment-specific variable from '.env' to process.env
const BigNumber = require('bignumber.js');

const smartContractsPaths = {
  "dev" : '/src/memefactory/shared/smart_contracts_dev.cljs',
  "qa" : '/src/memefactory/shared/smart_contracts_qa.cljs',
  "prod" :'/src/memefactory/shared/smart_contracts_prod.cljs'
};

let parameters = {
  "dev" : {
      polygonCheckpointManager: "0x597C411F86D674aa38dF42F76990ea57346978b2",
      fxRoot: "0x449949F4262510ffcf79402Abd547449bD3f3e07",
      fxChild: "0xAA2889D0923E6E00b111b2B291CB37f4878442f5",
      ENS: "0x00000000000C2E074eC69A0dFb2997BA6C7d2e1e",
      memeRegistryDb : {challengePeriodDuration : 600, // seconds (10 minutes)
        commitPeriodDuration : 600, // seconds
        revealPeriodDuration : 600, // seconds
        deposit : "1000000000000000000", // 1e18 = 1 DANK
        challengeDispensation : 50, // percent
        maxTotalSupply : 10, // int
        maxAuctionDuration : 1.21e6 // seconds
      },
      paramChangeRegistryDb : {challengePeriodDuration : 600, // seconds
        commitPeriodDuration : 600, // seconds
        revealPeriodDuration : 600, // seconds
        deposit : "1000000000000000000", // 1e18 = 1 DANK
        challengeDispensation : 50 // percent
      },
      dankFaucet : {dank : "5000000000000000000000000", // how much DANK contract holds, 1e18 = 1 DANK
        eth : "100000000000000000", // ETH, 1e18 = 1ETH
        allotment : "20000000000000000000",  // how much DANK faucet sends, 1e18 = 1 DANK
        sender: "0x0000000000000000000000000000000000000000"
      }
    },
  "qa" : {
    polygonCheckpointManager: "0x2890ba17efe978480615e330ecb65333b880928e",
    fxRoot: "0x3d1d3E34f7fB6D26245E6640E1c50710eFFf15bA",
    fxChild: "0xCf73231F28B7331BBe3124B907840A94851f9f11",
    ENS: "0x00000000000C2E074eC69A0dFb2997BA6C7d2e1e",
    memeRegistryDb : {challengePeriodDuration : 600, // seconds (10 minutes)
                      commitPeriodDuration : 600, // seconds
                      revealPeriodDuration : 600, // seconds
                      deposit : "1000000000000000000", // 1e18 = 1 DANK
                      challengeDispensation : 50, // percent
                      maxTotalSupply : 10, // int
                      maxAuctionDuration : 1.21e6 // seconds
                     },
    paramChangeRegistryDb : {challengePeriodDuration : 600, // seconds
                             commitPeriodDuration : 600, // seconds
                             revealPeriodDuration : 600, // seconds
                             deposit : "1000000000000000000", // 1e18 = 1 DANK
                             challengeDispensation : 50 // percent
                            },
    dankFaucet : {dank : "5000000000000000000000000", // how much DANK contract holds, 1e18 = 1 DANK
                  eth : "100000000000000000", // ETH, 1e18 = 1ETH
                  allotment : "20000000000000000000", // how much DANK faucet sends, 1e18 = 1 DANK
                  sender: "0x7532413a93eb86554c67fdd86eded3642cf68289"
    }
  },
  "prod" : {
    polygonCheckpointManager: "0x86E4Dc95c7FBdBf52e33D563BbDB00823894C287",
    fxRoot: "0xfe5e5D361b2ad62c541bAb87C45a0B9B018389a2",
    fxChild: "0x8397259c983751DAf40400790063935a11afa28a",
    ENS: "0x00000000000C2E074eC69A0dFb2997BA6C7d2e1e",
    memeRegistryDb : {challengePeriodDuration : 86400, // seconds (24h)
                      commitPeriodDuration : 86400, // seconds
                      revealPeriodDuration : 86400, // seconds
                      deposit : new BigNumber("100e18"), // 1e18 = 1 DANK
                      challengeDispensation : 50, // percent
                      maxTotalSupply : 100, // int
                      maxAuctionDuration : (30 * 86400) // seconds
                     },
    paramChangeRegistryDb : {challengePeriodDuration : 86400, // seconds
                             commitPeriodDuration : 86400, // seconds
                             revealPeriodDuration : 86400, // seconds
                             deposit : new BigNumber("1000000000e18"), // 1e18 = 1 DANK
                             challengeDispensation : 50 // percent
                            },
    dankFaucet : {dank : new BigNumber("5000000e18"), // how much DANK contract holds, 1e18 = 1 DANK
                  eth : new BigNumber("0.2e18"), // ETH, 1e18 = 1ETH
                  allotment : new BigNumber("2000e18"),  // how much DANK faucet sends, 1e18 = 1 DANK
                  sender: "0x0000000000000000000000000000000000000000"
    }
  }
};

module.exports = {
  env: MEMEFACTORY_ENV,
  smart_contracts_path: __dirname + smartContractsPaths [MEMEFACTORY_ENV],
  contracts_build_directory: __dirname + '/resources/public/contracts/build/',
  parameters : parameters [MEMEFACTORY_ENV],
  networks: {
    "ganache": {
      host: 'localhost',
      port: 8545,
      gas: 6e6, // gas limit
      gasPrice: 20e9, // 20 gwei, default for ganache
      network_id: '*'
    },
    "root": {
      networkCheckTimeout: 1000000,
      host: 'localhost',
      port: 9545,
      gas: 6e6, // gas limit
      gasPrice: 0,
      network_id: '*'
    },
    "bor": {
      host: 'localhost',
      port: 8545,
      network_id: '*',
      skipDryRun: true,
      gas: 7000000,
      gasPrice: 0
    },
    "infura-ropsten": {
      provider: () => new HDWalletProvider(process.env.ROPSTEN_PRIV_KEY, "https://ropsten.infura.io/v3/" + process.env.INFURA_API_KEY),
      network_id: 3,
      gas: 6e6,
      gasPrice: 6e9,
      skipDryRun: true
    },
    "infura-goerli": {
      provider: () => new HDWalletProvider(process.env.GOERLI_PRIV_KEY, "https://goerli.infura.io/v3/" + process.env.INFURA_API_KEY),
      network_id: 5,
      gas: 6e6,
      gasPrice: 6e9,
      skipDryRun: true
    },
    "infura-polygon-mumbai": {
      provider: () => new HDWalletProvider(process.env.POLYGON_MUMBAY_PRIV_KEY, "https://polygon-mumbai.infura.io/v3/" + process.env.INFURA_API_KEY),
      network_id: 80001,
      gas: 6e6,
      gasPrice: 9e9,
      skipDryRun: true
    },
    "infura-mainnet": {
      provider: () => new HDWalletProvider(process.env.MAINNET_PRIV_KEY, "https://mainnet.infura.io/v3/" + process.env.INFURA_API_KEY),
      network_id: 1,
      gas: 6e6,
      gasPrice: 9e9,
      skipDryRun: true
    },
    "infura-polygon-mainnet": {
      provider: () => new HDWalletProvider(process.env.POLYGON_MAINNET_PRIV_KEY, "https://polygon-mainnet.infura.io/v3/" + process.env.INFURA_API_KEY),
      network_id: 137,
      gas: 6e6,
      gasPrice: 9e9,
      skipDryRun: true
    }
  },
  compilers: {
      solc: {
        version: "0.8.2",
        settings: {
          optimizer: {
            enabled: true
          }
        }
      }
    }
};
