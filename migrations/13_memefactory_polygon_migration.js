// Fresh installation to L2 (polygon)
// it assumes an existing DANK and DSGuard contracts deployed on L1

const {readSmartContractsFile, getSmartContractAddress, copy, linkBytecode, smartContractsTemplate} = require ("./utils.js");
const fs = require('fs');
const edn = require("jsedn");
const {env, contracts_build_directory, smart_contracts_path, parameters} = require ('../truffle.js');
const web3Utils = require('web3-utils');

var smartContracts = readSmartContractsFile(smart_contracts_path);
var dankRoot = getSmartContractAddress(smartContracts, ":DANK-root");
if (!dankRoot)
    dankRoot = getSmartContractAddress(smartContracts, ":DANK");
var dsGuardRoot = getSmartContractAddress(smartContracts, ":ds-guard-root");
if (!dsGuardRoot)
    dsGuardRoot = getSmartContractAddress(smartContracts, ":ds-guard");

const Migrations = artifacts.require("Migrations");

copy ("DSGuard", "DSGuardCp", contracts_build_directory);
const DSGuard = artifacts.require("DSGuardCp");

copy ("MemeAuth", "MemeAuthCp", contracts_build_directory);
const MemeAuth = artifacts.require("MemeAuthCp");

copy ("MiniMeTokenFactory", "MiniMeTokenFactoryCp", contracts_build_directory);
const MiniMeTokenFactory = artifacts.require("MiniMeTokenFactoryCp");

copy ("DankTokenChild", "DankTokenChildCp", contracts_build_directory);
const DankTokenChild = artifacts.require("DankTokenChildCp");

copy ("DankChildController", "DankChildControllerCp", contracts_build_directory);
const DankChildController = artifacts.require("DankChildControllerCp");

copy ("DistrictConfig", "DistrictConfigCp", contracts_build_directory);
const DistrictConfig = artifacts.require("DistrictConfigCp");
copy ("District0xEmails", "District0xEmailsCp", contracts_build_directory);
const District0xEmails = artifacts.require ("District0xEmailsCp");

copy ("ParamChangeRegistry", "ParamChangeRegistryCp", contracts_build_directory);
const ParamChangeRegistry = artifacts.require("ParamChangeRegistryCp");

// copy artifacts for placeholder replacements
copy ("EternalDb", "MemeRegistryDb", contracts_build_directory);
const MemeRegistryDb = artifacts.require("MemeRegistryDb");

copy ("EternalDb", "ParamChangeRegistryDb", contracts_build_directory);
const ParamChangeRegistryDb = artifacts.require("ParamChangeRegistryDb");

copy ("Registry", "MemeRegistry", contracts_build_directory);
const MemeRegistry = artifacts.require("MemeRegistry");

copy ("MutableForwarder", "MemeRegistryForwarder", contracts_build_directory);
const MemeRegistryForwarder = artifacts.require("MemeRegistryForwarder");

copy ("MutableForwarder", "ParamChangeRegistryForwarder", contracts_build_directory);
const ParamChangeRegistryForwarder = artifacts.require("ParamChangeRegistryForwarder");

copy ("MemeTokenChild", "MemeTokenChildCp", contracts_build_directory);
const MemeTokenChild = artifacts.require("MemeTokenChildCp");

copy ("Meme", "MemeCp", contracts_build_directory);
const Meme = artifacts.require("MemeCp");

copy ("ParamChange", "ParamChangeCp", contracts_build_directory);
const ParamChange = artifacts.require("ParamChangeCp");

copy ("MemeFactory", "MemeFactoryCp", contracts_build_directory);
const MemeFactory = artifacts.require("MemeFactoryCp");

copy ("ParamChangeFactory", "ParamChangeFactoryCp", contracts_build_directory)
const ParamChangeFactory = artifacts.require("ParamChangeFactoryCp");

copy ("MutableForwarder", "MemeAuctionFactoryForwarder", contracts_build_directory);
const MemeAuctionFactoryForwarder = artifacts.require("MemeAuctionFactoryForwarder");

copy ("MemeAuctionFactory", "MemeAuctionFactoryCp", contracts_build_directory);
const MemeAuctionFactory = artifacts.require("MemeAuctionFactoryCp");

copy ("MemeAuction", "MemeAuctionCp", contracts_build_directory);
const MemeAuction = artifacts.require("MemeAuctionCp");

const registryPlaceholder = "feedfeedfeedfeedfeedfeedfeedfeedfeedfeed";
const dankTokenPlaceholder = "deaddeaddeaddeaddeaddeaddeaddeaddeaddead";
const forwarderTargetPlaceholder = "beefbeefbeefbeefbeefbeefbeefbeefbeefbeef";
const districtConfigPlaceholder = "abcdabcdabcdabcdabcdabcdabcdabcdabcdabcd";
const memeTokenPlaceholder = "dabbdabbdabbdabbdabbdabbdabbdabbdabbdabb";
const memeAuctionFactoryPlaceholder = "daffdaffdaffdaffdaffdaffdaffdaffdaffdaff";

/**
 * This migration deploys the MemeFactory smart contract suite
 *
 * Usage:
 * DEV
 * truffle migrate --network ganache --f 1 --to 3 --reset
 *
 * QA
 * env MEMEFACTORY_ENV=qa truffle migrate --network infura-ropsten --f 1 --to 3
 *
 * PROD
 * env MEMEFACTORY_ENV=prod truffle migrate --network infura-mainnet --f 1 --to 3
 */
module.exports = function(deployer, network, accounts) {

  const address = accounts [0];
  const gas = 4e6;
  const opts = {gas: gas, from: address};

  deployer.then (() => {
    console.log ("@@@ using Web3 version:", web3.version.api);
    console.log ("@@@ using address", address);
    console.log ("@@@ using smart contracts file", smart_contracts_path);
  });

  deployer.deploy (District0xEmails, Object.assign(opts, {gas: 500000}))
    .then (() => deployer.deploy (DSGuard, Object.assign(opts, {gas: gas})))
    .then (instance => {
      // make deployed :ds-guard its own autority
      return instance.setAuthority(instance.address, Object.assign(opts, {gas: 100000}));
    })
    .then(() => deployer.deploy (MiniMeTokenFactory, Object.assign(opts, {gas: gas})))
    .then (instance => deployer.deploy (DankTokenChild, instance.address, Object.assign(opts, {gas: gas})))
    .then (instance => instance.totalSupply())
    .then (res => console.log ("@@@ DankToken/totalSupply:", res))
    .then (() => deployer.deploy (DistrictConfig, address, address, 0, Object.assign(opts, {gas: 1000000})))
    .then (() => Promise.all([DSGuard.deployed(), DistrictConfig.deployed()]))
    .then ((
      [dSGuard,
       districtConfig]) => districtConfig.setAuthority(dSGuard.address, Object.assign(opts, {gas: 100000})))
    .then (() => deployer.deploy (MemeRegistryDb, Object.assign(opts, {gas: gas})))
    .then (() => deployer.deploy (ParamChangeRegistryDb, Object.assign(opts, {gas: gas})))
    .then (() => deployer.deploy (MemeRegistry, Object.assign(opts, {gas: gas})))
    .then (instance => {
      linkBytecode(MemeRegistryForwarder, forwarderTargetPlaceholder, instance.address);
      return deployer.deploy(MemeRegistryForwarder, Object.assign(opts, {gas: gas}))
    })
    .then (instance => instance.target())
    .then (res => console.log ("@@@ MemeRegistryForwarder target:",  res))
    .then (() => deployer.deploy (ParamChangeRegistry, Object.assign(opts, {gas: gas})))
    .then (instance => {
      linkBytecode(ParamChangeRegistryForwarder, forwarderTargetPlaceholder, instance.address);
      return deployer.deploy(ParamChangeRegistryForwarder, Object.assign(opts, {gas: gas}));
    })
    .then (() => Promise.all([MemeRegistryDb.deployed(), MemeRegistryForwarder.deployed()]))
    .then ((
      [memeRegistryDb,
       memeRegistryForwarder]) => {
         return MemeRegistry.at (memeRegistryForwarder.address)
         .then((target) => {
          return target.construct (memeRegistryDb.address, Object.assign(opts, {gas: 100000}));
         })
       })
    .then (() => MemeRegistryForwarder.deployed ())
    .then (instance => {
      return MemeRegistry.at (instance.address)
      .then((target) => {
        return target.db ();
      })
    })
    .then ( (res) => console.log ("@@@ MemeRegistry/db :", res))
    .then ( () => Promise.all ([ParamChangeRegistryDb.deployed (),
                                ParamChangeRegistryForwarder.deployed ()]))
    .then ((
      [paramChangeRegistryDb,
       paramChangeRegistryForwarder]) => {
         return ParamChangeRegistry.at (paramChangeRegistryForwarder.address)
         .then((target) => {
          return target.construct (paramChangeRegistryDb.address, Object.assign(opts, {gas: 100000}));
         })
       })
    .then (() => Promise.all(
      [DSGuard.deployed (),
       ParamChangeRegistryForwarder.deployed ()]))
    .then ((
      [dSGuard,
       paramChangeRegistryForwarder]) =>
           // Allow :param-change-registry-fwd to grand permissions to other contracts (for ParamChanges to apply changes)
           dSGuard.methods['permit(address,address,bytes32)'].sendTransaction(paramChangeRegistryForwarder.address, dSGuard.address, '0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff', Object.assign(opts, {gas: 100000})))
    .then (() => Promise.all([MemeRegistryForwarder.deployed (),
                                    DSGuard.deployed()]))
    .then ((
        [registry,
         dsGuard]) => deployer.deploy (MemeAuth, registry.address, dsGuard.address, Object.assign(opts, {gas: 3200000})))
    .then (() => deployer.deploy (MemeTokenChild, Object.assign(opts, {gas: 5200000})))
    .then (() => Promise.all([MemeAuth.deployed(), MemeTokenChild.deployed()]))
    .then ((
          [memeAuth,
           memeTokenChild]) => memeTokenChild.setAuthority(memeAuth.address, Object.assign(opts, {gas: 100000})))
    .then (() => Promise.all ([DankTokenChild.deployed (),
                               MemeRegistryForwarder.deployed (),
                               DistrictConfig.deployed (),
                               MemeTokenChild.deployed ()]))
    .then ((
      [dankTokenChild,
       memeRegistryForwarder,
       districtConfig,
       memeTokenChild]) => {
         linkBytecode(Meme, dankTokenPlaceholder, dankTokenChild.address);
         linkBytecode(Meme, registryPlaceholder, memeRegistryForwarder.address);
         linkBytecode(Meme, districtConfigPlaceholder, districtConfig.address);
         linkBytecode(Meme, memeTokenPlaceholder, memeTokenChild.address);
         return deployer.deploy(Meme, Object.assign(opts, {gas: 6721975}));
       })
    .then (() => Promise.all ([DankTokenChild.deployed (),
                               ParamChangeRegistryForwarder.deployed ()]))
    .then ((
      [dankTokenChild,
       paramChangeRegistryForwarder]) => {
         linkBytecode(ParamChange, dankTokenPlaceholder, dankTokenChild.address);
         linkBytecode(ParamChange, registryPlaceholder, paramChangeRegistryForwarder.address);
         return deployer.deploy (ParamChange, Object.assign(opts, {gas: 6000000}));
       })
    .then (() => Promise.all ([Meme.deployed (),
                               MemeRegistryForwarder.deployed (),
                               DankTokenChild.deployed (),
                               MemeTokenChild.deployed ()]))
    .then ((
      [meme,
       memeRegistryForwarder,
       dankTokenChild,
       memeTokenChild]) => {
         linkBytecode(MemeFactory, forwarderTargetPlaceholder, meme.address);
         return deployer.deploy (MemeFactory, memeRegistryForwarder.address, dankTokenChild.address, memeTokenChild.address,
                                 Object.assign(opts, {gas: gas}));
       })
    .then (() => {
      return Promise.all ([ParamChange.deployed (),
                           ParamChangeRegistryForwarder.deployed (),
                           DankTokenChild.deployed ()]);
    })
    .then ((
      [paramChange,
       paramChangeRegistryForwarder,
       dankTokenChild]) => {
         linkBytecode(ParamChangeFactory, forwarderTargetPlaceholder, paramChange.address);
         return deployer.deploy (ParamChangeFactory, paramChangeRegistryForwarder.address, dankTokenChild.address,
                                 Object.assign(opts, {gas: gas}));
       })
    .then (() => MemeRegistryDb.deployed ())
    .then ((instance) => instance.setUIntValues (['challengePeriodDuration',
                                                  'commitPeriodDuration',
                                                  'revealPeriodDuration',
                                                  'deposit',
                                                  'challengeDispensation',
                                                  'maxTotalSupply',
                                                  'maxAuctionDuration'].map((k) => {return web3Utils.soliditySha3(k);}),
                                                 [parameters.memeRegistryDb.challengePeriodDuration,
                                                  parameters.memeRegistryDb.commitPeriodDuration,
                                                  parameters.memeRegistryDb.revealPeriodDuration ,
                                                  parameters.memeRegistryDb.deposit,
                                                  parameters.memeRegistryDb.challengeDispensation,
                                                  parameters.memeRegistryDb.maxTotalSupply,
                                                  parameters.memeRegistryDb.maxAuctionDuration],
                                                 Object.assign(opts, {gas: 500000})))
    .then (() => MemeRegistryDb.deployed ())
    .then (instance => instance.getUIntValue (web3Utils.soliditySha3 ("deposit")))
    .then (res => console.log ("MemeRegistryDb/deposit:", res))
    .then (() => ParamChangeRegistryDb.deployed ())
    .then ((instance) => instance.setUIntValues (['challengePeriodDuration',
                                                  'commitPeriodDuration',
                                                  'revealPeriodDuration',
                                                  'deposit',
                                                  'challengeDispensation'].map((k) => {return web3Utils.soliditySha3(k);}),
                                                 [parameters.paramChangeRegistryDb.challengePeriodDuration,
                                                  parameters.paramChangeRegistryDb.commitPeriodDuration,
                                                  parameters.paramChangeRegistryDb.revealPeriodDuration ,
                                                  parameters.paramChangeRegistryDb.deposit  ,
                                                  parameters.paramChangeRegistryDb.challengeDispensation],
                                                 Object.assign(opts, {gas: 500000})))
    .then (() => Promise.all ([DSGuard.deployed (),
                               MemeRegistryDb.deployed ()]))
    .then ((
      [dSGuard,
       memeRegistryDb]) => //  make :ds-guard authority of both :meme-registry-db and :param-change-registry-db
           memeRegistryDb.setAuthority(dSGuard.address, Object.assign(opts, {gas: 100000})))
    .then (() => Promise.all ([DSGuard.deployed (),
                               ParamChangeRegistryDb.deployed ()]))
    .then ((
      [dSGuard,
       paramChangeRegistryDb]) => //  make :ds-guard authority of both :meme-registry-db and :param-change-registry-db
           paramChangeRegistryDb.setAuthority(dSGuard.address, Object.assign(opts, {gas: 100000})))
    .then (() => Promise.all ([MemeRegistryDb.deployed (),
                               ParamChangeRegistryDb.deployed ()]))
    .then ((
      [memeRegistryDb,
       paramChangeRegistryDb]) => // after authority is set, we can clean owner. Not really essential, but extra safety measure
           Promise.all ([memeRegistryDb.setOwner("0x0000000000000000000000000000000000000000", Object.assign(opts, {gas: 100000})),
                         paramChangeRegistryDb.setOwner ("0x0000000000000000000000000000000000000000", Object.assign(opts, {gas: 100000}))]))
    .then (() => Promise.all([DSGuard.deployed (),
                              MemeRegistryForwarder.deployed (),
                              MemeRegistryDb.deployed ()]))
    .then ((
      [dSGuard,
       memeRegistryForwarder,
       memeRegistryDb]) => // allow :meme-registry-fwd to make changes into :meme-registry-db
           dSGuard.methods['permit(address,address,bytes32)'].sendTransaction(memeRegistryForwarder.address, memeRegistryDb.address, '0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff', Object.assign(opts, {gas: 100000})))
    .then (() => Promise.all ([DSGuard.deployed (),
                               ParamChangeRegistryForwarder.deployed (),
                               MemeRegistryDb.deployed ()]))

    .then ((
      [dSGuard,
       paramChangeRegistryForwarder,
       memeRegistryDb]) => // allow :param-change-registry-fwd to make changes into :meme-registry-db (to apply ParamChanges)
           dSGuard.methods['permit(address,address,bytes32)'].sendTransaction(paramChangeRegistryForwarder.address, memeRegistryDb.address, '0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff', Object.assign(opts, {gas: 100000})))
    .then (() => Promise.all ([DSGuard.deployed (),
                               ParamChangeRegistryForwarder.deployed (),
                               ParamChangeRegistryDb.deployed ()]))
    .then ((
      [dSGuard,
       paramChangeRegistryForwarder,
       paramChangeRegistryDb]) => // allow :param-change-registry-fwd to make changes into :param-change-registry-db
           dSGuard.methods['permit(address,address,bytes32)'].sendTransaction(paramChangeRegistryForwarder.address, paramChangeRegistryDb.address, '0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff', Object.assign(opts, {gas: 100000})))

    .then (() => Promise.all ([MemeRegistryForwarder.deployed (),
                               MemeFactory.deployed ()]))
    .then ((
      [memeRegistryForwarder,
       memeFactory]) => {
         return MemeRegistry.at (memeRegistryForwarder.address)
         .then((target) => {
          return target.setFactory (memeFactory.address, true, Object.assign(opts, {gas: 100000}));
         })
       })
    .then (() => Promise.all ([MemeRegistryForwarder.deployed (),
                               MemeFactory.deployed ()]))
    .then ((
      [memeRegistryForwarder,
       memeFactory]) => {
         return MemeRegistry.at (memeRegistryForwarder.address)
         .then((target) => {
          return target.isFactory (memeFactory.address);
         })
       })
    .then (res => console.log ("@@@ MemeRegistry/isFactory", res))
    .then (() => Promise.all ([ParamChangeRegistryForwarder.deployed (),
                               ParamChangeFactory.deployed ()]))
    .then (([paramChangeRegistryForwarder,
             paramChangeFactory]) => {
               return ParamChangeRegistry.at (paramChangeRegistryForwarder.address)
               .then((target) => {
                return target.setFactory (paramChangeFactory.address, true, Object.assign(opts, {gas: 100000}));
               })
             })
    .then (() => {
      return deployer.deploy (MemeAuctionFactoryForwarder, Object.assign(opts, {gas: gas}));
    }).then (() => Promise.all ([DSGuard.deployed (),
                                 MemeAuctionFactoryForwarder.deployed ()]))
    .then ((
      [dSGuard,
       memeAuctionFactoryForwarder]) => memeAuctionFactoryForwarder.setAuthority(dSGuard.address, Object.assign(opts, {gas: 100000})))
    .then (() => Promise.all ([MemeAuctionFactoryForwarder.deployed (),
                               MemeRegistryForwarder.deployed (),
                               DistrictConfig.deployed (),
                               MemeTokenChild.deployed ()]))
    .then ((
      [memeAuctionFactoryForwarder,
       memeRegistryForwarder,
       districtConfig,
       memeTokenChild]
    ) => {
      linkBytecode(MemeAuction, memeAuctionFactoryPlaceholder, memeAuctionFactoryForwarder.address);
      linkBytecode(MemeAuction, registryPlaceholder, memeRegistryForwarder.address);
      linkBytecode(MemeAuction, districtConfigPlaceholder, districtConfig.address);
      linkBytecode(MemeAuction, memeTokenPlaceholder, memeTokenChild.address);
      return deployer.deploy(MemeAuction, Object.assign(opts, {gas: 4000000}));
    })
    .then ((instance) => {
      linkBytecode(MemeAuctionFactory, forwarderTargetPlaceholder, instance.address);
      return Promise.all ([deployer.deploy(MemeAuctionFactory, Object.assign(opts, {gas: 2000000})),
                           MemeAuctionFactoryForwarder.deployed ()]);
    }).then ((
      [memeAuctionFactory,
       memeAuctionFactoryForwarder]) => memeAuctionFactoryForwarder.setTarget(memeAuctionFactory.address, Object.assign(opts, {gas: 100000})))
    .then (() => Promise.all([MemeTokenChild.deployed (),
                              MemeAuctionFactoryForwarder.deployed ()]))
    .then ((
      [memeTokenChild,
       memeAuctionFactoryForwarder]) => {
         return MemeAuctionFactory.at (memeAuctionFactoryForwarder.address)
         .then((target) => {
          return target.construct (memeTokenChild.address, Object.assign(opts, {gas: 200000}));
         })
       })
    .then (() => DankTokenChild.deployed())
    .then ((dankTokenChild) => {
      return deployer.deploy(DankChildController, dankTokenChild.address, Object.assign(opts, {gas: 4000000}));
    })
    .then (() => Promise.all ([DankChildController.deployed(), DankTokenChild.deployed()]))
    .then (([dankChildController, dankTokenChild]) => {
      return dankTokenChild.changeController(dankChildController.address, Object.assign(opts, {gas: 100000}));
    })
    .then (() => Promise.all ([DankChildController.deployed(), DSGuard.deployed()]))
    .then (([dankChildController, dSGuard]) => Promise.all ([dankChildController.setAuthority(dSGuard.address, Object.assign(opts, {gas: 200000, value: 0})),
                                                            dankChildController]))
    .then (([tx,
             dankChildController
            ]) => dankChildController.authority ())
    .then ((authority) => console.log ("@@@ DankChildController authority: ", authority))
    .then ( () => [
      Migrations.deployed(),
      DSGuard.deployed (),
      MemeAuth.deployed (),
      MiniMeTokenFactory.deployed (),
      DankTokenChild.deployed (),
      DistrictConfig.deployed (),
      MemeRegistryDb.deployed (),
      ParamChangeRegistryDb.deployed (),
      MemeRegistry.deployed (),
      ParamChangeRegistry.deployed (),
      MemeRegistryForwarder.deployed (),
      ParamChangeRegistryForwarder.deployed (),
      MemeTokenChild.deployed (),
      Meme.deployed (),
      ParamChange.deployed (),
      MemeFactory.deployed (),
      ParamChangeFactory.deployed (),
      MemeAuctionFactoryForwarder.deployed (),
      MemeAuctionFactory.deployed (),
      MemeAuction.deployed (),
      District0xEmails.deployed (),
      DankChildController.deployed ()])
    .then ((promises) => Promise.all(promises))
    .then ((
      [migrations,
       dSGuard,
       memeAuth,
       miniMeTokenFactory,
       dankTokenChild,
       districtConfig,
       memeRegistryDb,
       paramChangeRegistryDb,
       memeRegistry,
       paramChangeRegistry,
       memeRegistryForwarder,
       paramChangeRegistryForwarder,
       memeTokenChild,
       meme,
       paramChange,
       memeFactory,
       paramChangeFactory,
       memeAuctionFactoryForwarder,
       memeAuctionFactory,
       memeAuction,
       district0xEmails,
       dankChildController]) => {

         var smartContracts = edn.encode(
           new edn.Map([

             edn.kw(":migrations"), new edn.Map([edn.kw(":name"), "Migrations",
                                                 edn.kw(":address"), migrations.address]),

             edn.kw(":district-config"), new edn.Map([edn.kw(":name"), "DistrictConfig",
                                                      edn.kw(":address"), districtConfig.address]),

             edn.kw(":ds-guard"), new edn.Map([edn.kw(":name"), "DSGuard",
                                               edn.kw(":address"), dSGuard.address]),

             edn.kw(":ds-guard-root"), new edn.Map([edn.kw(":name"), "DSGuard",
                                               edn.kw(":address"), dsGuardRoot]),

             edn.kw(":meme-auth"), new edn.Map([edn.kw(":name"), "MemeAuth",
                                               edn.kw(":address"), memeAuth.address]),

             edn.kw(":param-change-registry"), new edn.Map([edn.kw(":name"), "ParamChangeRegistry",
                                                            edn.kw(":address"), paramChangeRegistry.address]),

             edn.kw(":param-change-registry-db"), new edn.Map([edn.kw(":name"), "EternalDb",
                                                               edn.kw(":address"), paramChangeRegistryDb.address]),

             edn.kw(":meme-registry-db"), new edn.Map([edn.kw(":name"), "EternalDb",
                                                       edn.kw(":address"), memeRegistryDb.address]),

             edn.kw(":param-change"), new edn.Map([edn.kw(":name"), "ParamChange",
                                                   edn.kw(":address"), paramChange.address]),

             edn.kw(":minime-token-factory"), new edn.Map([edn.kw(":name"), "MiniMeTokenFactory",
                                                           edn.kw(":address"), miniMeTokenFactory.address]),

             edn.kw(":meme-auction-factory"), new edn.Map([edn.kw(":name"), "MemeAuctionFactory",
                                                           edn.kw(":address"), memeAuctionFactory.address]),

             edn.kw(":meme-auction"), new edn.Map([edn.kw(":name"), "MemeAuction",
                                                   edn.kw(":address"), memeAuction.address]),

             edn.kw(":param-change-factory"), new edn.Map([edn.kw(":name"), "ParamChangeFactory",
                                                           edn.kw(":address"), paramChangeFactory.address]),

             edn.kw(":param-change-registry-fwd"), new edn.Map([edn.kw(":name"), "MutableForwarder",
                                                                edn.kw(":address"), paramChangeRegistryForwarder.address,
                                                                edn.kw(":forwards-to"), edn.kw(":param-change-registry")]),

             edn.kw(":meme-factory"), new edn.Map([edn.kw(":name"), "MemeFactory",
                                                   edn.kw(":address"), memeFactory.address]),

             edn.kw(":meme-token-root"), new edn.Map([edn.kw(":name"), "MemeToken",
                                                 edn.kw(":address"), "0x0000000000000000000000000000000000000000"]),

             edn.kw(":meme-token"), new edn.Map([edn.kw(":name"), "MemeTokenChild",
                                                 edn.kw(":address"), memeTokenChild.address]),

             edn.kw(":DANK"), new edn.Map([edn.kw(":name"), "DankTokenChild",
                                           edn.kw(":address"), dankTokenChild.address]),

             edn.kw(":DANK-root"), new edn.Map([edn.kw(":name"), "DankToken",
                                           edn.kw(":address"), dankRoot]),

             edn.kw(":meme-registry"), new edn.Map([edn.kw(":name"), "Registry",
                                                    edn.kw(":address"), memeRegistry.address]),

             edn.kw(":meme"), new edn.Map([edn.kw(":name"), "Meme",
                                           edn.kw(":address"), meme.address]),

             edn.kw(":meme-registry-fwd"), new edn.Map([edn.kw(":name"), "MutableForwarder",
                                                        edn.kw(":address"), memeRegistryForwarder.address,
                                                        edn.kw(":forwards-to"), edn.kw(":meme-registry")]),

             edn.kw(":meme-auction-factory-fwd"), new edn.Map([edn.kw(":name"), "MutableForwarder",
                                                               edn.kw(":address"), memeAuctionFactoryForwarder.address,
                                                               edn.kw(":forwards-to"), edn.kw(":meme-auction-factory")]),

             edn.kw(":district0x-emails"), new edn.Map([edn.kw(":name"), "District0xEmails",
                                                        edn.kw(":address"), district0xEmails.address]),

             edn.kw(":ens"), new edn.Map([edn.kw(":name"), "ENS",
                                                        edn.kw(":address"), parameters.ENS]),

             edn.kw(":DANK-child-controller"), new edn.Map([edn.kw(":name"), "DankChildController",
                                                  edn.kw(":address"), dankChildController.address]),

             edn.kw(":DANK-root-tunnel"), new edn.Map([edn.kw(":name"), "DankRootTunnel",
                 edn.kw(":address"), "0x0000000000000000000000000000000000000000"]),

             edn.kw(":DANK-child-tunnel"), new edn.Map([edn.kw(":name"), "DankChildTunnel",
                 edn.kw(":address"), "0x0000000000000000000000000000000000000000"]),

             edn.kw(":meme-token-root-tunnel"), new edn.Map([edn.kw(":name"), "MemeTokenRootTunnel",
                 edn.kw(":address"), "0x0000000000000000000000000000000000000000"]),

             edn.kw(":meme-token-child-tunnel"), new edn.Map([edn.kw(":name"), "MemeTokenChildTunnel",
                 edn.kw(":address"), "0x0000000000000000000000000000000000000000"]),

           ]));

         console.log (smartContracts);
         fs.writeFileSync(smart_contracts_path, smartContractsTemplate (smartContracts, env));
       })
    .catch(console.error);

  deployer.then (function () {
    console.log ("Done");
  });

}