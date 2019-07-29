const {readSmartContractsFile,getSmartContractAddress, setSmartContractAddress, writeSmartContracts, linkBytecode} = require ("./utils.js");
const {parameters, smart_contracts_path, env} = require ('../truffle.js');
const web3Utils = require('web3-utils');

const ParamChangeFactory = artifacts.require("ParamChangeFactory");
const ParamChange = artifacts.require("ParamChange");
const ParamChangeRegistry = artifacts.require("ParamChangeRegistry");
const MutableForwarder = artifacts.require("MutableForwarder");

console.log("Using smart_contracts file ", smart_contracts_path);

var smartContracts = readSmartContractsFile(smart_contracts_path);
var dankTokenAddr = getSmartContractAddress(smartContracts, ":DANK");
var paramChangeRegistryForwarderAddr = getSmartContractAddress(smartContracts, ":param-change-registry-fwd");
var paramChangeRegistryDbAddr = getSmartContractAddress(smartContracts, ":param-change-registry-db");

const forwarderTargetPlaceholder = "beefbeefbeefbeefbeefbeefbeefbeefbeefbeef";
const registryPlaceholder = "feedfeedfeedfeedfeedfeedfeedfeedfeedfeed";
const dankTokenPlaceholder = "deaddeaddeaddeaddeaddeaddeaddeaddeaddead";

/**
 * This migration redeploys ParamChange.sol ParamChangeFactory.sol ParamChangeRegistry.sol
 * which were updated in 10742ba
 *
 * Usage:
 * truffle migrate --network ganache/parity --reset --f 7 --to 7
 */
module.exports = function(deployer, network, accounts) {

  const address = accounts [0];
  const gas = 4e6;
  const opts = {gas: gas, from: address};

  deployer.then (() => {
    console.log ("@@@ using Web3 version:", web3.version.api);
    console.log ("@@@ using address", address);
    console.log ("Current ParamChange address", getSmartContractAddress(smartContracts, ":param-change"));
    console.log ("Current ParamChangeRegistry address", getSmartContractAddress(smartContracts, ":param-change-registry"));
    console.log ("Current ParamChangeFactory address", getSmartContractAddress(smartContracts, ":param-change-factory"));
  });

  deployer
  ////////////////////////
  // Deploy ParamChange //
  ////////////////////////
    .then (() => {
      linkBytecode(ParamChange, dankTokenPlaceholder, dankTokenAddr);
      linkBytecode(ParamChange, registryPlaceholder, paramChangeRegistryForwarderAddr);
      return deployer.deploy (ParamChange, Object.assign(opts, {gas: 6000000}));
    })
  ////////////////////////////////
  // Deploy ParamChangeRegistry //
  ////////////////////////////////
    .then (() => deployer.deploy (ParamChangeRegistry, Object.assign(opts, {gas: gas})))
    .then (() => Promise.all([ParamChangeRegistry.deployed (),
                              ParamChange.deployed ()]))
  /////////////////////////////////////////////////////////////////////////////////////
  // Set ParamChangeRegistryForwarder to new location and deploy ParamChange Factory //
  /////////////////////////////////////////////////////////////////////////////////////
    .then (async ([paramChangeRegInstance, paramChangeInstance]) => {
      var newParamChangeRegistryAddress = paramChangeRegInstance.address;
      var newParamChangeAddress = paramChangeInstance.address;

      var paramChangeRegFwdInstance = MutableForwarder.at(paramChangeRegistryForwarderAddr);

      var targetBefore = ParamChangeRegistry.at(paramChangeRegistryForwarderAddr);
      console.log("Target before", targetBefore.target());
      // Point to the ParamChangeRegistryForwarder to new location
      await paramChangeRegFwdInstance.setTarget(newParamChangeRegistryAddress);

      var target = ParamChangeRegistry.at(paramChangeRegistryForwarderAddr);

      console.log("About to construct the registry");
      console.log("DB addr", paramChangeRegistryDbAddr);
      console.log("Forwarder instance ", paramChangeRegFwdInstance.address);
      console.log("Is now pointing to  ", newParamChangeRegistryAddress);
      console.log("Target after", target.target());
      await target.construct (paramChangeRegistryDbAddr, Object.assign(opts, {gas: 800000}));

      console.log("HERE we are!!!!");
      // Write the new addresses on the file
      setSmartContractAddress(smartContracts, ":param-change-registry", newParamChangeRegistryAddress);
      console.log("New ParamChangeRegistry address is ", newParamChangeRegistryAddress);

      setSmartContractAddress(smartContracts, ":param-change", newParamChangeAddress);
      console.log("New ParamChange address is ", newParamChangeAddress);

      // Deploy the new ParamChangeFactory
      linkBytecode(ParamChangeFactory, forwarderTargetPlaceholder, newParamChangeAddress);
      return deployer.deploy (ParamChangeFactory, paramChangeRegistryForwarderAddr, dankTokenAddr,
                              Object.assign(opts, {gas: gas}));
    })
    .then((paramChangeFactoryInstance) => {
      var target = ParamChangeRegistry.at(paramChangeRegistryForwarderAddr);
      target.setFactory(paramChangeFactoryInstance.address, true, Object.assign(opts, {gas: 100000}));

      setSmartContractAddress(smartContracts, ":param-change-factory", paramChangeFactoryInstance.address);
      console.log("New ParamChangeFactory address is ", paramChangeFactoryInstance.address);

      /////////////////////////////
      // Write the new addresses //
      /////////////////////////////
      writeSmartContracts(smart_contracts_path, smartContracts, env);

    })
    .catch(console.error);

  deployer.then (function () {
    console.log ("Done");
  });

}
