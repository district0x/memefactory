const {last, copy, linkBytecode, smartContractsTemplate} = require ("./utils.js");
const {contracts_build_directory, smart_contracts_path, parameters} = require ('../truffle.js');

const MEMEFACTORY_ENV = process.env.MEMEFACTORY_ENV || "dev";

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

/**
 * This migration updates Meme and ParamChange Contracts
 *
 * Usage:
 * truffle migrate --network ganache/parity --reset --f 5 --to 5
 */
module.exports = function(deployer, network, accounts) {

  const address = accounts [0];
  const gas = 4e6;
  const opts = {gas: gas, from: address, gasPrice: 10e9};

  // IMPORTANT : adjust these addresses :
  const deployedMemeFactoryAddress = {
    "dev": "0x9c176f70828ca1de41d31e5f4fa137c4c4743dae",
    "prod" : "0x01cb025ec5d7907e33b357bccae6260e9adbd32a",
    "qa" : "0x1c4144670e895384d7f0a3ae2e4aec2833c1fbf8"
  };

  const deployedDistrictConfigAddress = {
    "dev" : "0xf446f195ec4ad452f79f95ccb27cfa5dd6cb9e97",
    "prod": "0xc3f953d1d9c0117f0988a16f2eda8641467e0b6d",
    "qa" : "0xc0631861f334e80e960da6317f8b66a122b32e71"
  };

  const deployedParamChangeFactoryAddress = {
    "dev" : "0x9909f7dc7c04dace5d376334fa6afac0e919477b",
    "prod" : "0x179921d3a4b673581c68b21631aa7573b651d4e5",
    "qa": "0x6446ae75abdef8ff35a20e49499c0f54e278f067"
  };

  const deployedParamChangeRegistryForwarderAddress = {
    "dev" : "0x2f94d13d398e615798b5fe2c4b5d55425b23feb7",
    "prod" : "0x942b6b83b654761b13fba7b230b9283ddec08f2c",
    "qa" : "0x5091c87601b085d5abb477a534bcac80fd11896e"
  };

  deployer.then (() => {
    console.log ("@@@ using Web3 version:", web3.version.api);
    console.log ("@@@ MEMEFACTORY_ENV: ", MEMEFACTORY_ENV);
    console.log ("@@@ using address", address);
  });

  deployer
    .then(() => OldMemeFactory.at(deployedMemeFactoryAddress [MEMEFACTORY_ENV]))
    .then((oldMemeFactory) => {
      return Promise.all([
        oldMemeFactory.registry(),
        oldMemeFactory.registryToken(),
        oldMemeFactory.memeToken(),
        OldDistrictConfig.at(deployedDistrictConfigAddress[MEMEFACTORY_ENV])
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

      linkBytecode(MemeFactory, forwarderTargetPlaceholder, meme.address);
      var memeFactory = deployer.deploy(MemeFactory, memeRegistryAddress, dankTokenAddress, memeTokenAddress);

      return Promise.all([
        memeFactory,
        OldMemeFactory.at(deployedMemeFactoryAddress[MEMEFACTORY_ENV]),
        MemeRegistry.at(memeRegistryAddress)
      ]);
    })
    .then(([memeFactory, oldMemeFactory, memeRegistry]) => {
      console.log("@@@ New MemeFactory: ", memeFactory.address);

      return Promise.all([
        memeRegistry.setFactory(memeFactory.address, true, Object.assign(opts, {gas: 100000})),
        memeRegistry.setFactory(oldMemeFactory.address, false, Object.assign(opts, {gas: 100000}))
      ]);
    })

  // Param Change

    .then(() => OldParamChangeFactory.at(deployedParamChangeFactoryAddress[MEMEFACTORY_ENV]))
    .then((oldParamChangeFactory) => Promise.all([
      oldParamChangeFactory.registry(),
      oldParamChangeFactory.registryToken()
    ]))
    .then(([paramChangeRegistryAddress, dankTokenAddress]) => Promise.all([
      ParamChangeRegistryForwarder.at (deployedParamChangeRegistryForwarderAddress[MEMEFACTORY_ENV]),
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

      linkBytecode(ParamChangeFactory, forwarderTargetPlaceholder, paramChange.address);
      var paramChangeFactory = deployer.deploy(ParamChangeFactory, paramChangeRegistryForwarder.address, dankTokenAddress);

      return Promise.all([
        paramChangeFactory,
        OldParamChangeFactory.at(deployedParamChangeFactoryAddress[MEMEFACTORY_ENV]),
        ParamChangeRegistry.at(paramChangeRegistryForwarder.address)
      ]);
    })
    .then(([paramChangeFactory, oldParamChangeFactory, paramChangeRegistry]) => {
      console.log("@@@ New ParamChangeFactory: ", paramChangeFactory.address);

      return Promise.all([
        paramChangeRegistry.setFactory(paramChangeFactory.address, true, Object.assign(opts, {gas: 100000})),
        paramChangeRegistry.setFactory(oldParamChangeFactory.address, false, Object.assign(opts, {gas: 100000}))
      ]);
    })
    .then (() => console.log ("Done"))
}
