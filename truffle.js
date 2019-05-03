'use strict';

module.exports = {
  smart_contracts_path: __dirname + '/src/memefactory/shared/smart_contracts_dev.cljs',
  contracts_build_directory: __dirname + '/resources/public/contracts/build/',
  parameters : {
    memeRegistryDb : {challengePeriodDuration : 86400, // seconds
                      commitPeriodDuration : 86400, // seconds
                      revealPeriodDuration : 86400, // seconds
                      deposit : 100e18, // 1e18 = 1 DANK
                      challengeDispensation : 50, // percent
                      voteQuorum : 50, // percent
                      maxTotalSupply : 100, // int
                      maxAuctionDuration : (* 30 86400) // seconds
                     },
    paramChangeRegistryDb : {challengePeriodDuration : 86400, // seconds
                             commitPeriodDuration : 86400, // seconds
                             revealPeriodDuration : 86400, // seconds
                             deposit : 2000e18, // 1e18 = 1 DANK
                             challengeDispensation : 50, // percent
                             voteQuorum : 50 // percent
                            },
    dankFaucet : {dank : 5000000e18, // how much DANK contract holds, 1e18 = 1 DANK
                  eth : 2e18, // ETH, 1e18 = 1ETH
                  allotment : 2000e18  // how much DANK faucet sends, 1e18 = 1 DANK
                 }},
  networks: {
    ganache: {
      host: 'localhost',
      port: 8549,
      gas: 8e6, // gas limit
      gasPrice: 2e10, // 20 gwei, default for ganache
      network_id: '*'
    },
    parity: {
      host: 'localhost',
      port: 8545,
      gas: 8e6,
      gasPrice: 4e9, // 4 gwei
      network_id: '*'
    }
  }
};
