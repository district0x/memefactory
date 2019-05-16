const {last, copy, linkBytecode, smartContractsTemplate} = require ("./utils.js");
const {contracts_build_directory, smart_contracts_path, parameters} = require ('../truffle.js');

copy ("MutableForwarder", "MemeRegistryForwarder", contracts_build_directory);
const MemeRegistryForwarder = artifacts.require("MemeRegistryForwarder");

copy ("MutableForwarder", "ParamChangeRegistryForwarder", contracts_build_directory);
const ParamChangeRegistryForwarder = artifacts.require("ParamChangeRegistryForwarder");

/**
 * Fixes Registry contruct function
 *
 * Usage:
 * truffle migrate --network ganache/parity --reset --f 4 --to 4
 */
module.exports = function(deployer, network, accounts) {

  const address = accounts [0];
  const gas = 4e6;
  const opts = {gas: gas, from: address, gasPrice: 20e9};

  deployer.then (() => {
    console.log ("@@@ using Web3 version:", web3.version.api);
    console.log ("@@@ using address", address);
  });

  deployer
    .then (() => {
      var memeRegistryForwarder = MemeRegistryForwarder.at("0xe278b85a36f6b370347d69fb4744947e2965c058");
      return memeRegistryForwarder.setTarget("0x8f24fb009e0ed2a342d56b5246ef69f72d297744", opts);
    })
    .then (() => {
      var paramChangeRegistryForwarder = ParamChangeRegistryForwarder.at("0x942b6b83b654761b13fba7b230b9283ddec08f2c");
      return paramChangeRegistryForwarder.setTarget("0x1fa85cbb83b065e5eb3ea302dc49a9cde7a5d47f" , opts);
    })
    .then (function () {
      console.log ("Done");
    });
}
