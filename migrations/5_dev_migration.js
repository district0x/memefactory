const {last, copy, linkBytecode, smartContractsTemplate, readSmartContractsFile, getSmartContractAddress, setSmartContractAddress, writeSmartContracts} = require ("./utils.js");
const {contracts_build_directory, smart_contracts_path, parameters, env} = require ('../truffle.js');

// existing contracts
const OldMemeFactory = artifacts.require("MemeFactory");
const OldDistrictConfig = artifacts.require("DistrictConfig");
const OldParamChangeFactory = artifacts.require("ParamChangeFactory");

copy ("Registry", "MemeRegistry", contracts_build_directory);
const MemeRegistry = artifacts.require("MemeRegistry");

copy ("MutableForwarder", "ParamChangeRegistryForwarder", contracts_build_directory);
const ParamChangeRegistryForwarder = artifacts.require("ParamChangeRegistryForwarder");

const ParamChangeRegistry = artifacts.require("ParamChangeRegistry");

// redeployed contracts

copy ("MemeFactory", "MemeFactoryCp", contracts_build_directory);
const MemeFactory = artifacts.require("MemeFactoryCp");

copy ("ParamChangeFactory", "ParamChangeFactoryCp", contracts_build_directory)
const ParamChangeFactory = artifacts.require("ParamChangeFactoryCp");

copy ("Meme", "MemeCp", contracts_build_directory);
const Meme = artifacts.require("MemeCp");

copy ("ParamChange", "ParamChangeCp", contracts_build_directory);
const ParamChange = artifacts.require("ParamChangeCp");

const registryPlaceholder = "feedfeedfeedfeedfeedfeedfeedfeedfeedfeed";
const dankTokenPlaceholder = "deaddeaddeaddeaddeaddeaddeaddeaddeaddead";
const forwarderTargetPlaceholder = "beefbeefbeefbeefbeefbeefbeefbeefbeefbeef";
const districtConfigPlaceholder = "abcdabcdabcdabcdabcdabcdabcdabcdabcdabcd";
const memeTokenPlaceholder = "dabbdabbdabbdabbdabbdabbdabbdabbdabbdabb";

const smartContracts = readSmartContractsFile(smart_contracts_path);

const deployedMemeFactoryAddress = getSmartContractAddress(smartContracts, ":meme-factory")
const deployedDistrictConfigAddress = getSmartContractAddress(smartContracts, ":district-config");
const deployedParamChangeFactoryAddress = getSmartContractAddress(smartContracts, ":param-change-factory");
const deployedParamChangeRegistryForwarderAddress = getSmartContractAddress(smartContracts, ":param-change-registry-fwd");

/**
 * This migration updates Meme and ParamChange Contracts
 * truffle migrate --network ganache --f 5 --to 5
 */
module.exports = function(deployer, network, accounts) {

  const address = accounts [0];
  const gas = 4e6;
  const opts = {gas: gas, from: address, gasPrice: 10e9};

  deployer.then (() => {
    console.log ("@@@ using Web3 version:", web3.version.api);
    console.log ("@@@ using address", address);
    console.log ("@@@ using smart contracts file", smart_contracts_path);
  });

  deployer
    .then (() => {
      console.log ("@@@ using MemeFactory at: ", deployedMemeFactoryAddress);
      console.log ("@@@ using DistrictConfig at: ", deployedDistrictConfigAddress );
      console.log ("@@@ using ParamChangeFactory at: ", deployedParamChangeFactoryAddress );
      console.log ("@@@ using ParamChangeRegistryForwarder at: ", deployedParamChangeRegistryForwarderAddress );
    })
    .then(() => OldMemeFactory.at(deployedMemeFactoryAddress ))
    .then((oldMemeFactory) => {
      return Promise.all([
        oldMemeFactory.registry(),
        oldMemeFactory.registryToken(),
        oldMemeFactory.memeToken(),
        OldDistrictConfig.at(deployedDistrictConfigAddress)
      ])
    })
    .then(([memeRegistryAddress, dankTokenAddress, memeTokenAddress, districtConfig]) => {
      console.log ("@@@ MemeRegistryForwarder: ", memeRegistryAddress);
      console.log ("@@@ DankToken: ", dankTokenAddress);
      console.log ("@@@ MemeToken: ", memeTokenAddress);
      console.log ("@@@ DistrictConfig: ", districtConfig.address);

      linkBytecode(Meme, registryPlaceholder, memeRegistryAddress);
      linkBytecode(Meme, dankTokenPlaceholder, dankTokenAddress);
      linkBytecode(Meme, memeTokenPlaceholder, memeTokenAddress);
      linkBytecode(Meme, districtConfigPlaceholder, districtConfig.address);

      var meme = deployer.deploy(Meme, Object.assign(opts, {gas: 6721975}));

      return Promise.all([
        meme,
        memeRegistryAddress,
        dankTokenAddress,
        memeTokenAddress
      ])
    })
    .then(([meme, memeRegistryAddress, dankTokenAddress, memeTokenAddress]) => {

      console.log("@@@ New Meme: ", meme.address);
      setSmartContractAddress(smartContracts, ":meme", meme.address);

      linkBytecode(MemeFactory, forwarderTargetPlaceholder, meme.address);
      var memeFactory = deployer.deploy(MemeFactory, memeRegistryAddress, dankTokenAddress, memeTokenAddress);

      return Promise.all([
        memeFactory,
        OldMemeFactory.at(deployedMemeFactoryAddress),
        MemeRegistry.at(memeRegistryAddress)
      ]);
    })
    .then(([memeFactory, oldMemeFactory, memeRegistry]) => {

      console.log("@@@ New MemeFactory: ", memeFactory.address);
      setSmartContractAddress(smartContracts, ":meme-factory", memeFactory.address);

      return Promise.all([
        memeRegistry.setFactory(memeFactory.address, true, Object.assign(opts, {gas: 100000})),
        memeRegistry.setFactory(oldMemeFactory.address, false, Object.assign(opts, {gas: 100000}))
      ]);
    })

  // Param Change

    .then(() => OldParamChangeFactory.at(deployedParamChangeFactoryAddress))
    .then((oldParamChangeFactory) => Promise.all([
      oldParamChangeFactory.registry(),
      oldParamChangeFactory.registryToken()
    ]))
    .then(([paramChangeRegistryAddress, dankTokenAddress]) => Promise.all([
      ParamChangeRegistryForwarder.at (deployedParamChangeRegistryForwarderAddress),
      dankTokenAddress
    ]))
    .then(([paramChangeRegistryForwarder, dankTokenAddress]) => {
      console.log("@@@ ParamChangeRegistryForwarder: ", paramChangeRegistryForwarder.address);
      console.log("@@@ DankToken: ", dankTokenAddress);

      linkBytecode(ParamChange, registryPlaceholder, paramChangeRegistryForwarder.address);
      linkBytecode(ParamChange, dankTokenPlaceholder, dankTokenAddress);

      var paramChange = deployer.deploy(ParamChange, Object.assign(opts, {gas: 6000000}));

      return Promise.all([
        paramChange,
        paramChangeRegistryForwarder,
        dankTokenAddress
      ])
    })
    .then(([paramChange, paramChangeRegistryForwarder, dankTokenAddress]) => {

      console.log("@@@ New Param Change: ", paramChange.address);
      setSmartContractAddress(smartContracts, ":param-change", paramChange.address);

      linkBytecode(ParamChangeFactory, forwarderTargetPlaceholder, paramChange.address);
      var paramChangeFactory = deployer.deploy(ParamChangeFactory, paramChangeRegistryForwarder.address, dankTokenAddress);

      return Promise.all([
        paramChangeFactory,
        OldParamChangeFactory.at(deployedParamChangeFactoryAddress),
        ParamChangeRegistry.at(paramChangeRegistryForwarder.address)
      ]);
    })
    .then(([paramChangeFactory, oldParamChangeFactory, paramChangeRegistry]) => {

      console.log("@@@ New ParamChangeFactory: ", paramChangeFactory.address);
      setSmartContractAddress(smartContracts, ":param-change-factory", paramChangeFactory.address);

      return Promise.all([
        paramChangeRegistry.setFactory(paramChangeFactory.address, true, Object.assign(opts, {gas: 100000})),
        paramChangeRegistry.setFactory(oldParamChangeFactory.address, false, Object.assign(opts, {gas: 100000}))
      ]);
    })
    .then (() => {
      writeSmartContracts(smart_contracts_path, smartContracts, env);
      console.log ("Done")
    });
}
