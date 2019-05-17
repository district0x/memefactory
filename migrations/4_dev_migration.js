const {last, copy, linkBytecode, smartContractsTemplate} = require ("./utils.js");
const {contracts_build_directory, smart_contracts_path, parameters} = require ('../truffle.js');

copy ("Registry", "MemeRegistry", contracts_build_directory);
const MemeRegistry = artifacts.require("MemeRegistry");

// copy ("ParamChangeRegistry", "ParamChangeRegistryCp", contracts_build_directory);
const ParamChangeRegistry = artifacts.require("ParamChangeRegistry");

copy ("MutableForwarder", "MemeRegistryForwarder", contracts_build_directory);
const MemeRegistryForwarder = artifacts.require("MemeRegistryForwarder");

copy ("MutableForwarder", "ParamChangeRegistryForwarder", contracts_build_directory);
const ParamChangeRegistryForwarder = artifacts.require("ParamChangeRegistryForwarder");

/**
 * This migration updates Registry contracts
 *
 * Usage:
 * truffle migrate --network ganache/parity --reset --f 4 --to 4
 */
module.exports = function(deployer, network, accounts) {

  const address = accounts [0];
  const gas = 4e6;
  const opts = {gas: gas, from: address, gasPrice: 30e9};

  // IMPORTANT : change these addresses :
  const memeRegistryForwarderAddress = "0x70a955eb4bdf84652c7586945176cfe58529711d";
  const paramChangeRegistryForwarderAddress = "0x89219dbec1f6f69677765b6d205052cf17a91926";

  deployer.then (() => {
    console.log ("@@@ using Web3 version:", web3.version.api);
    console.log ("@@@ using address", address);
  });

  deployer.deploy (MemeRegistry, Object.assign(opts, {gas: gas}))
    .then (() => deployer.deploy (ParamChangeRegistry, Object.assign(opts, {gas: gas})))
    .then (() => Promise.all(
      [MemeRegistry.deployed (),
       MemeRegistryForwarder.at (memeRegistryForwarderAddress),
       ParamChangeRegistry.deployed (),
       ParamChangeRegistryForwarder.at (paramChangeRegistryForwarderAddress)]))
    .then (([memeRegistry,
             memeRegistryFwd,
             paramChangeRegistry,
             paramChangeRegistryFwd]) => Promise.all ([memeRegistryFwd.setTarget(memeRegistry.address, opts),
                                                       paramChangeRegistryFwd.setTarget(paramChangeRegistry.address, opts)]))
    .then (() => Promise.all(
      [MemeRegistry.deployed (),
       ParamChangeRegistry.deployed ()]))
    .then (([memeRegistry, paramChangeRegistry]) =>
           {
             console.log ("@@@ MemeRegistry address:", memeRegistry.address);
             console.log ("@@@ ParamChangeRegistry address:", paramChangeRegistry.address);
           })
    .then ( () => console.log ("Done"));

}
