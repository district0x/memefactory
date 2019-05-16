const {last, copy, linkBytecode, smartContractsTemplate} = require ("./utils.js");
const {contracts_build_directory, smart_contracts_path, parameters} = require ('../truffle.js');

copy ("Registry", "MemeRegistry", contracts_build_directory);
const MemeRegistry = artifacts.require("MemeRegistry");

copy ("ParamChangeRegistry", "ParamChangeRegistryCp", contracts_build_directory);
const ParamChangeRegistry = artifacts.require("ParamChangeRegistryCp");

/**
 * Fixes Registry contruct function
 *
 * Usage:
 * truffle migrate --network ganache/parity --reset --f 4 --to 4
 */
module.exports = function(deployer, network, accounts) {

  const address = accounts [0];
  const gas = 4e6;
  const opts = {gas: gas, from: address, gasPrice: 30e9};

  deployer.then (() => {
    console.log ("@@@ using Web3 version:", web3.version.api);
    console.log ("@@@ using address", address);
  });

  deployer.deploy (MemeRegistry, Object.assign(opts, {gas: gas}))
    .then (() => deployer.deploy (ParamChangeRegistry, Object.assign(opts, {gas: gas})))
    .then (() => Promise.all(
          [MemeRegistry.deployed (),
           ParamChangeRegistry.deployed ()]))
    .then (([dSGuard, memeRegistryDb]) =>
       {
           console.log ("@@@ MemeRegistry:", MemeRegistry.address);
           console.log ("@@@ ParamChangeRegistry:", ParamChangeRegistry.address);
       })
    .then (function () {
      console.log ("Done");
  });

}
