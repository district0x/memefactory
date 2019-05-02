'use strict';

module.exports = {
  smart_contracts_path: __dirname + '/src/memefactory/shared/smart_contracts_dev.cljs',
  contracts_build_directory: __dirname + '/resources/public/contracts/build/',
  parameters : {
    memeRegistryDb : {challengePeriodDuration : 600, // seconds
                      commitPeriodDuration : 600, // seconds
                      revealPeriodDuration : 600, // seconds
                      deposit : 1e18, // 1e18 = 1 DANK
                      challengeDispensation : 50, // percent
                      voteQuorum : 50, // percent
                      maxTotalSupply : 10, // int
                      maxAuctionDuration : 1.21e6 // seconds
                     },
    paramChangeRegistryDb : {challengePeriodDuration : 600, // seconds
                             commitPeriodDuration : 600, // seconds
                             revealPeriodDuration : 600, // seconds
                             deposit : 1e18, // 1e18 = 1 DANK
                             challengeDispensation : 50, // percent
                             voteQuorum : 50 // percent
                            },
    dankFaucet : {dank : 5000000e18, // 1e18 = 1 DANK
                  eth : 2.0 // ETH
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
