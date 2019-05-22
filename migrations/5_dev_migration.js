const {last, copy, linkBytecode, smartContractsTemplate} = require ("./utils.js");
const {contracts_build_directory, smart_contracts_path, parameters} = require ('../truffle.js');

const MEMEFACTORY_ENV = process.env.MEMEFACTORY_ENV || "dev";

const OldMemeFactory = artifacts.require("MemeFactoryCp");
const OldParamChangeFactory = artifacts.require("ParamChangeFactoryCp");
const OldParamChange = artifacts.require("ParamChangeCp");

const MemeRegistry = artifacts.require("MemeRegistry");
const ParamChangeRegistry = artifacts.require("ParamChangeRegistry");
const ParamChangeRegistryForwarder = artifacts.require("ParamChangeRegistryForwarder");

const MemeToken = artifacts.require("MemeTokenCp");
const DankToken = artifacts.require("DankTokenCp");
const DistrictConfig = artifacts.require("DistrictConfigCp");

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


function getOldMemeFactory() {
  var contract;
  switch(MEMEFACTORY_ENV) {
    case "prod":
      contract = MemeFactory.at("0x01cb025ec5d7907e33b357bccae6260e9adbd32a");
      break;
    case "qa":
      contract = MemeFactory.at("0x1c4144670e895384d7f0a3ae2e4aec2833c1fbf8");
      break;
    default:
      contract = OldMemeFactory.deployed();
  }
  return contract;
}

function getOldParamChangeFactory() {
  var contract;
  switch(MEMEFACTORY_ENV) {
    case "prod":
      contract = ParamChangeFactory.at("0x179921d3a4b673581c68b21631aa7573b651d4e5");
      break;
    case "qa":
      contract = ParamChangeFactory.at("0x6446ae75abdef8ff35a20e49499c0f54e278f067");
      break;
    default:
      contract = OldParamChangeFactory.deployed();
  }
  return contract;
}

function getDistrictConfig() {
  var contract;
  switch(MEMEFACTORY_ENV) {
    case "prod":
      contract = DistrictConfig.at("0xc3f953d1d9c0117f0988a16f2eda8641467e0b6d");
      break;
    case "qa":
      contract = DistrictConfig.at("0xc0631861f334e80e960da6317f8b66a122b32e71");
      break;
    default:
      contract = DistrictConfig.deployed();
  }
  return contract;
}

function getParamChangeRegistryForwarder(paramChangeRegistryAddress) {
  var contract;
  switch(MEMEFACTORY_ENV) {
    case "prod":
      contract = ParamChangeRegistryForwarder.at("0x942b6b83b654761b13fba7b230b9283ddec08f2c");
      break;
    case "qa":
      contract = ParamChangeRegistryForwarder("0x5091c87601b085d5abb477a534bcac80fd11896e");
      break;
    default:
    
    // This is hack, because originally we had bug in out deployment migration.
    // We didn't assign to ParamChangeFactory.registry address of ParamChangeRegistryForwarder,
    // but original ParamChangeRegistry contract address.
    // https://github.com/district0x/memefactory/blob/8c961070086f7e5f860310c016d55d6843bcf01a/migrations/2_memefactory_migration.js#L199
    // Therefore wanna get here ParamChangeRegistryForwarder address, but first we must linkBytecode again,
    // so truffle can find it within blockchain.
        
    linkBytecode(ParamChangeRegistryForwarder, forwarderTargetPlaceholder, paramChangeRegistryAddress);
    contract = ParamChangeRegistryForwarder.deployed();
  }
  return contract;
}

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

  deployer.then (() => {
    console.log ("@@@ using Web3 version:", web3.version.api);
    console.log ("@@@ MEMEFACTORY_ENV: ", MEMEFACTORY_ENV);
    console.log ("@@@ using address", address);
  });

  deployer
    .then(() => getOldMemeFactory())
    .then((oldMemeFactory) => {
      return Promise.all([
         oldMemeFactory.registry(),
         oldMemeFactory.registryToken(),
         oldMemeFactory.memeToken(),
         getDistrictConfig(),
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
         getOldMemeFactory(),
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


    .then(() => getOldParamChangeFactory())
    .then((oldParamChangeFactory) => {
      return Promise.all([
         oldParamChangeFactory.registry(),
         oldParamChangeFactory.registryToken()
      ])
    })
    .then(([paramChangeRegistryAddress, dankTokenAddress]) => {

    return Promise.all([
       getParamChangeRegistryForwarder(paramChangeRegistryAddress),
       dankTokenAddress
      ]);
    })
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
         getOldParamChangeFactory(),
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
    .then (() => {
      console.log ("Done");
    })
}
