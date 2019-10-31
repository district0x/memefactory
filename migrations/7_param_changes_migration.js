const web3Utils = require('web3-utils');
const { readSmartContractsFile, getSmartContractAddress, setSmartContractAddress, writeSmartContracts, linkBytecode } = require ("./utils.js");
const { parameters, smart_contracts_path, env } = require ('../truffle.js');

const ParamChangeFactory = artifacts.require("ParamChangeFactory");
const ParamChange = artifacts.require("ParamChange");
const ParamChangeRegistry = artifacts.require("ParamChangeRegistry");
const MutableForwarder = artifacts.require("MutableForwarder");
const Migrations = artifacts.require("Migrations");

var smartContracts = readSmartContractsFile(smart_contracts_path);
var dankTokenAddr = getSmartContractAddress(smartContracts, ":DANK");
var paramChangeRegistryForwarderAddr = getSmartContractAddress(smartContracts, ":param-change-registry-fwd");
var paramChangeRegistryDbAddr = getSmartContractAddress(smartContracts, ":param-change-registry-db");
const migrationsAddress = getSmartContractAddress(smartContracts, ":migrations");

const forwarderTargetPlaceholder = "beefbeefbeefbeefbeefbeefbeefbeefbeefbeef";
const registryPlaceholder = "feedfeedfeedfeedfeedfeedfeedfeedfeedfeed";
const dankTokenPlaceholder = "deaddeaddeaddeaddeaddeaddeaddeaddeaddead";

/**
 * This migration redeploys ParamChange.sol ParamChangeFactory.sol ParamChangeRegistry.sol
 * which were updated in 10742ba
 * truffle migrate --network ganache --f 7 --to 7
 */
module.exports = function(deployer, network, accounts) {

  const address = accounts [0];
  const gas = 4e6;
  const opts = {gas: gas, from: address};

  deployer.then (() => {
    console.log ("@@@ using Web3 version:", web3.version.api);
    console.log ("@@@ using address", address);
    console.log ("@@@ using smart_contracts file ", smart_contracts_path);
    console.log ("@@@ current ParamChange address", getSmartContractAddress(smartContracts, ":param-change"));
    console.log ("@@@ current ParamChangeRegistry address", getSmartContractAddress(smartContracts, ":param-change-registry"));
    console.log ("@@@ current ParamChangeFactory address", getSmartContractAddress(smartContracts, ":param-change-factory"));
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

      // Point to the ParamChangeRegistryForwarder to new location
      console.log("Pointing ParamChangeRegFwd to new ParamChangeRegistryAddress : " ,newParamChangeRegistryAddress);
      await paramChangeRegFwdInstance.setTarget(newParamChangeRegistryAddress);
      console.log("Done.");

      var target = ParamChangeRegistry.at(paramChangeRegistryForwarderAddr);

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
    .then(async (paramChangeFactoryInstance) => {
      var target = ParamChangeRegistry.at(paramChangeRegistryForwarderAddr);
      await target.setFactory(paramChangeFactoryInstance.address, true, Object.assign(opts, {gas: 100000}));

      setSmartContractAddress(smartContracts, ":param-change-factory", paramChangeFactoryInstance.address);
      console.log("New ParamChangeFactory address is ", paramChangeFactoryInstance.address);

    })
    .then (async () => {

      // set last ran tx
      const migrations = Migrations.at (migrationsAddress);
      await migrations.setCompleted (7, Object.assign(opts, {gas: 100000}));

      writeSmartContracts(smart_contracts_path, smartContracts, env);
      console.log ("Done");
    });

}
