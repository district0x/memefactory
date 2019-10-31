const {readSmartContractsFile,getSmartContractAddress, setSmartContractAddress, writeSmartContracts, linkBytecode, copy} = require ("./utils.js");
const {contracts_build_directory, parameters, smart_contracts_path, env} = require ('../truffle.js');

copy ("ParamChangeFactory", "ParamChangeFactoryCp", contracts_build_directory);
const ParamChangeFactory = artifacts.require("ParamChangeFactoryCp");

copy ("ParamChange", "ParamChangeCp", contracts_build_directory);
const ParamChange = artifacts.require("ParamChangeCp");

copy ("ParamChangeRegistry", "ParamChangeRegistryCp", contracts_build_directory);
const ParamChangeRegistry = artifacts.require("ParamChangeRegistryCp");

copy ("MemeFactory", "MemeFactoryCp", contracts_build_directory);
const MemeFactory = artifacts.require("MemeFactoryCp");

copy ("Meme", "MemeCp", contracts_build_directory);
const Meme = artifacts.require("MemeCp");

copy ("Registry", "RegistryCp", contracts_build_directory);
const MemeRegistry = artifacts.require("RegistryCp");

const Migrations = artifacts.require("Migrations");

var smartContracts = readSmartContractsFile(smart_contracts_path);
var dankTokenAddr = getSmartContractAddress(smartContracts, ":DANK");
var memeRegistryForwarderAddr = getSmartContractAddress(smartContracts, ":meme-registry-fwd");
var paramChangeRegistryForwarderAddr = getSmartContractAddress(smartContracts, ":param-change-registry-fwd");
var districtConfigAddr = getSmartContractAddress(smartContracts, ":district-config");
var memeTokenAddr = getSmartContractAddress(smartContracts, ":meme-token");
const migrationsAddress = getSmartContractAddress(smartContracts, ":migrations");

const registryPlaceholder = "feedfeedfeedfeedfeedfeedfeedfeedfeedfeed";
const dankTokenPlaceholder = "deaddeaddeaddeaddeaddeaddeaddeaddeaddead";
const memeTokenPlaceholder = "dabbdabbdabbdabbdabbdabbdabbdabbdabbdabb";
const districtConfigPlaceholder = "abcdabcdabcdabcdabcdabcdabcdabcdabcdabcd";
const forwarderTargetPlaceholder = "beefbeefbeefbeefbeefbeefbeefbeefbeefbeef";

/**
 * This migration redeploys Meme.sol, ParamChange.sol, Memefactory.sol and ParamChangeFactory.sol
 * to remove vote quorum parameter
 *
 * env MEMEFACTORY_ENV=dev/qa/prod truffle migrate --network infura-ropsten --f 10 --to 10
 */
module.exports = function(deployer, network, accounts) {

  const address = accounts [0];
  const gas = 4e6;
  const opts = {gas: gas, from: address};

  deployer.then (() => {
    console.log ("@@@ using Web3 version:", web3.version.api);
    console.log ("@@@ using address", address);
    console.log("@@@ using smart_contracts file ", smart_contracts_path);
  })
    .then (async () => {

      // Deploy Meme
      linkBytecode(Meme, dankTokenPlaceholder, dankTokenAddr);
      linkBytecode(Meme, registryPlaceholder, memeRegistryForwarderAddr);
      linkBytecode(Meme, districtConfigPlaceholder, districtConfigAddr);
      linkBytecode(Meme, memeTokenPlaceholder, memeTokenAddr);
      var memeInstance = await deployer.deploy(Meme, Object.assign(opts, {gas: 6721975}));

      setSmartContractAddress(smartContracts, ":meme", memeInstance.address);

      // Deploy MemeFactory
      linkBytecode(MemeFactory, forwarderTargetPlaceholder, memeInstance.address);
      var memeFactoryInstance = await deployer.deploy (MemeFactory, memeRegistryForwarderAddr, dankTokenAddr, memeTokenAddr,
                                                       Object.assign(opts, {gas: gas}));

      setSmartContractAddress(smartContracts, ":meme-factory", memeFactoryInstance.address);

      // Deploy ParamChange
      linkBytecode(ParamChange, dankTokenPlaceholder, dankTokenAddr);
      linkBytecode(ParamChange, registryPlaceholder, paramChangeRegistryForwarderAddr);
      var paramChangeInstance = await deployer.deploy (ParamChange, Object.assign(opts, {gas: 6000000}));

      setSmartContractAddress(smartContracts, ":param-change", paramChangeInstance.address);

      // Deploy ParamChangeFactory
      linkBytecode(ParamChangeFactory, forwarderTargetPlaceholder, paramChangeInstance.address);
      var paramChangeFactoryInstance = await deployer.deploy (ParamChangeFactory, paramChangeRegistryForwarderAddr, dankTokenAddr,
                                                              Object.assign(opts, {gas: gas}));

      setSmartContractAddress(smartContracts, ":param-change-factory", paramChangeFactoryInstance.address);

      // Point both registries to the new deployed factories
      var memeRegistry = MemeRegistry.at(memeRegistryForwarderAddr);
      var paramChangeRegistry = ParamChangeRegistry.at(paramChangeRegistryForwarderAddr);

      await memeRegistry.setFactory(memeFactoryInstance.address, true, Object.assign(opts, {gas: 100000}))
      await paramChangeRegistry.setFactory(paramChangeFactoryInstance.address, true, Object.assign(opts, {gas: 100000}))

    })
    .then (async () => {

      // set last ran tx
      const migrations = Migrations.at (migrationsAddress);
      await migrations.setCompleted (10, Object.assign(opts, {gas: 100000}));

      writeSmartContracts(smart_contracts_path, smartContracts, env);
      console.log ("Done");
    });

}
