const {readSmartContractsFile, getSmartContractAddress, setSmartContractAddress, writeSmartContracts, linkBytecode} = require ("./utils.js");

const {parameters, smart_contracts_path, env} = require ('../truffle.js');
const web3Utils = require('web3-utils');

var smartContracts = readSmartContractsFile(smart_contracts_path);
var memeRegistryDbAddr = getSmartContractAddress(smartContracts, ":meme-registry-db");
var paramChangeRegistryDbAddr = getSmartContractAddress(smartContracts, ":param-change-registry-db");
var dSGuardAddr = getSmartContractAddress(smartContracts, ":ds-guard");

const EternalDb = artifacts.require("EternalDb");
const DSGuard = artifacts.require("DSGuard");

/**
 * This migration fixes MemeRegistryDb and ParamChangeRegistryDb keys that where encoded with
 * incorrect sha3 function. See https://github.com/district0x/memefactory/issues/505
 * truffle migrate --network ganache --f 6 --to 6
 */
module.exports = function(deployer, network, accounts) {

  const address = accounts [0];
  const gas = 4e6;
  const opts = {gas: gas, from: address};

  deployer.then (() => {
    console.log ("@@@ using Web3 version:", web3.version.api);
    console.log ("@@@ using address", address);
  })
    .then (async () => {
      var memeRegistryDbInstance = EternalDb.at(memeRegistryDbAddr);
      var dSGurardInstance = DSGuard.at(dSGuardAddr);

      await dSGurardInstance.permit(address, memeRegistryDbAddr, '0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff', Object.assign(opts, {gas: 100000}));

      console.log("Setting values for MemeRegistryDb");
      await memeRegistryDbInstance.setUIntValues (['challengePeriodDuration',
                                                   'commitPeriodDuration',
                                                   'revealPeriodDuration',
                                                   'deposit',
                                                   'challengeDispensation',
                                                   'voteQuorum',
                                                   'maxTotalSupply',
                                                   'maxAuctionDuration'].map((k) => {return web3Utils.soliditySha3(k);}),
                                                  [parameters.memeRegistryDb.challengePeriodDuration,
                                                   parameters.memeRegistryDb.commitPeriodDuration,
                                                   parameters.memeRegistryDb.revealPeriodDuration ,
                                                   parameters.memeRegistryDb.deposit  ,
                                                   parameters.memeRegistryDb.challengeDispensation,
                                                   parameters.memeRegistryDb.voteQuorum,
                                                   parameters.memeRegistryDb.maxTotalSupply,
                                                   parameters.memeRegistryDb.maxAuctionDuration],
                                                  Object.assign(opts, {gas: 500000}));

      await dSGurardInstance.forbid(address, memeRegistryDbAddr, '0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff', Object.assign(opts, {gas: 100000}));

      var paramChangeRegistryDbInstance = EternalDb.at(paramChangeRegistryDbAddr);

      await dSGurardInstance.permit(address, paramChangeRegistryDbAddr, '0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff', Object.assign(opts, {gas: 100000}));

      console.log("Setting values for ParamChangeRegistryDb");
      await paramChangeRegistryDbInstance.setUIntValues (['challengePeriodDuration',
                                                          'commitPeriodDuration',
                                                          'revealPeriodDuration',
                                                          'deposit',
                                                          'challengeDispensation',
                                                          'voteQuorum'].map((k) => {return web3Utils.soliditySha3(k);}),
                                                         [parameters.paramChangeRegistryDb.challengePeriodDuration,
                                                          parameters.paramChangeRegistryDb.commitPeriodDuration,
                                                          parameters.paramChangeRegistryDb.revealPeriodDuration ,
                                                          parameters.paramChangeRegistryDb.deposit  ,
                                                          parameters.paramChangeRegistryDb.challengeDispensation,
                                                          parameters.paramChangeRegistryDb.voteQuorum],
                                                         Object.assign(opts, {gas: 500000}));

      await dSGurardInstance.forbid(address, paramChangeRegistryDbAddr, '0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff', Object.assign(opts, {gas: 100000}));
    })
    .then (function () {
      console.log ("Done");
    });

}
