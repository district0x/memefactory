const {last, copy, linkBytecode, smartContractsTemplate, readSmartContractsFile, getSmartContractAddress, setSmartContractAddress, writeSmartContracts} = require ("./utils.js");
const {contracts_build_directory, smart_contracts_path, parameters, env} = require ('../truffle.js');

copy ("Registry", "MemeRegistry", contracts_build_directory);
const MemeRegistry = artifacts.require("MemeRegistry");

// copy ("ParamChangeRegistry", "ParamChangeRegistryCp", contracts_build_directory);
const ParamChangeRegistry = artifacts.require("ParamChangeRegistry");

copy ("MutableForwarder", "MemeRegistryForwarder", contracts_build_directory);
const MemeRegistryForwarder = artifacts.require("MemeRegistryForwarder");

copy ("MutableForwarder", "ParamChangeRegistryForwarder", contracts_build_directory);
const ParamChangeRegistryForwarder = artifacts.require("ParamChangeRegistryForwarder");

const smartContracts = readSmartContractsFile(smart_contracts_path);
const memeRegistryForwarderAddress = getSmartContractAddress(smartContracts, ":meme-registry-fwd");
const paramChangeRegistryForwarderAddress = getSmartContractAddress(smartContracts, ":param-change-registry-fwd");

/**
 * This migration updates Registry contracts
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

             setSmartContractAddress(smartContracts, ":meme-registry", memeRegistry.address);
             setSmartContractAddress(smartContracts, ":param-change-registry", paramChangeRegistry.address);

             writeSmartContracts(smart_contracts_path, smartContracts, env);
           })
    .then ( () => console.log ("Done"));

}
