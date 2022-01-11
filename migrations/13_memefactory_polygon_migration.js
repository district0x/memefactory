// Fresh installation to L2 (polygon)
// it assumes an existing DANK and DSGuard contracts deployed on L1

const {readSmartContractsFile, getSmartContractAddress, copy, linkBytecode, smartContractsTemplate} = require ("./utils.js");
const fs = require('fs');
const edn = require("jsedn");
const {env, contracts_build_directory, smart_contracts_path, parameters} = require ('../truffle.js');
const web3Utils = require('web3-utils');
const {Status} = require("../migrations/utils.js");

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
 * truffle migrate --network bor --f 13 --to 13 --reset
 *
 * QA
 * env MEMEFACTORY_ENV=qa truffle migrate --network infura-polygon-mumbai --f 13 --to 31
 *
 * PROD
 * env MEMEFACTORY_ENV=prod truffle migrate --network infura-polygon-mainnet --f 13 --to 13
 */
module.exports = async(deployer, network, accounts) => {

  const address = accounts [0];
  const gas = 4e6;
  const opts = {gas: gas, from: address};

  await deployer;

  console.log ("@@@ using Web3 version:", web3.version.api);
  console.log ("@@@ using address", address);
  console.log ("@@@ using smart contracts file", smart_contracts_path);

  const migrations = await Migrations.deployed();

  let status = new Status("13");

  // enum for status entries
  const sk = {
    district0xEmailsAddr: "district0xEmailsAddr",
    dsGuardAddr: "dsGuardAddr",
    miniMeTokenFactoryAddr: "miniMeTokenFactoryAddr",
    dankTokenChildAddr: "dankTokenChildAddr",
    districtConfigAddr: "districtConfigAddr",
    memeRegistryDbAddr: "memeRegistryDbAddr",
    paramChangeRegistryDbAddr: "paramChangeRegistryDbAddr",
    memeRegistryAddr: "memeRegistryAddr",
    memeRegistryForwarderAddr: "memeRegistryForwarderAddr",
    paramChangeRegistryAddr: "paramChangeRegistryAddr",
    paramChangeRegistryForwarderAddr: "paramChangeRegistryForwarderAddr",
    memeAuthAddr: "memeAuthAddr",
    memeTokenChildAddr: "memeTokenChildAddr",
    memeAddr: "memeAddr",
    paramChangeAddr: "paramChangeAddr",
    memeFactoryAddr: "memeFactoryAddr",
    paramChangeFactoryAddr: "paramChangeFactoryAddr",
    memeAuctionFactoryForwarderAddr: "memeAuctionFactoryForwarderAddr",
    memeAuctionAddr: "memeAuctionAddr",
    memeAuctionFactoryAddr: "memeAuctionFactoryAddr",
    dankChildControllerAddr: "dankChildControllerAddr"
  }

  await status.step(async ()=> {
    const district0xEmails = await deployer.deploy (District0xEmails, Object.assign(opts, {gas: 500000}));
    return {[sk.district0xEmailsAddr]: district0xEmails.address};
  });

  await status.step(async ()=> {
    const dsGuard = await deployer.deploy (DSGuard, Object.assign(opts, {gas: gas}));
    return {[sk.dsGuardAddr]: dsGuard.address};
  });

  await status.step(async ()=> {
    const dsGuardAddr = status.getValue(sk.dsGuardAddr);
    const dsGuard = await DSGuard.at(dsGuardAddr);
    // make deployed :ds-guard its own autority
    dsGuard.setAuthority(dsGuardAddr, Object.assign(opts, {gas: 100000}));
  });

  await status.step(async ()=> {
    const miniMeTokenFactory = await deployer.deploy (MiniMeTokenFactory, Object.assign(opts, {gas: gas}));
    return {[sk.miniMeTokenFactoryAddr] : miniMeTokenFactory.address}
  });

  await status.step(async ()=> {
    const miniMeTokenFactoryAddr = status.getValue(sk.miniMeTokenFactoryAddr);

    const dankTokenChild = await deployer.deploy (DankTokenChild, miniMeTokenFactoryAddr, Object.assign(opts, {gas: gas}));
    const totalSupply = await dankTokenChild.totalSupply();
    console.log ("@@@ DankToken/totalSupply:", totalSupply);
    return {[sk.dankTokenChildAddr]: dankTokenChild.address};
  });

  await status.step(async ()=> {
    const districtConfig = await deployer.deploy (DistrictConfig, address, address, 0, Object.assign(opts, {gas: 1000000}));
    return {[sk.districtConfigAddr]: districtConfig.address};
  });

  await status.step(async ()=> {
    const dsGuardAddr = status.getValue(sk.dsGuardAddr);
    const districtConfigAddr = status.getValue(sk.districtConfigAddr);
    const districtConfig = await DistrictConfig.at(districtConfigAddr);

    await districtConfig.setAuthority(dsGuardAddr, Object.assign(opts, {gas: 100000}));
  });

  await status.step(async ()=> {
    const memeRegistryDb = await deployer.deploy (MemeRegistryDb, Object.assign(opts, {gas: gas}));
    return {[sk.memeRegistryDbAddr]: memeRegistryDb.address}
  });

  await status.step(async ()=> {
    const paramChangeRegistryDb = await deployer.deploy (ParamChangeRegistryDb, Object.assign(opts, {gas: gas}));
    return {[sk.paramChangeRegistryDbAddr]: paramChangeRegistryDb.address};
  });

  await status.step(async ()=> {
    const memeRegistry = await deployer.deploy (MemeRegistry, Object.assign(opts, {gas: gas}));
    return {[sk.memeRegistryAddr]: memeRegistry.address};
  });

  await status.step(async ()=> {
    const memeRegistryAddr = status.getValue(sk.memeRegistryAddr);

    linkBytecode(MemeRegistryForwarder, forwarderTargetPlaceholder, memeRegistryAddr);
    const memeRegistryForwarder = await deployer.deploy(MemeRegistryForwarder, Object.assign(opts, {gas: gas}));
    const target = await memeRegistryForwarder.target();
    console.log ("@@@ MemeRegistryForwarder target:",  target);
    return {[sk.memeRegistryForwarderAddr]: memeRegistryForwarder.address};
  });

  await status.step(async ()=> {
    const paramChangeRegistry = await deployer.deploy (ParamChangeRegistry, Object.assign(opts, {gas: gas}));
    return {[sk.paramChangeRegistryAddr]: paramChangeRegistry.address};
  });

  await status.step(async ()=> {
    const paramChangeRegistryAddr = status.getValue(sk.paramChangeRegistryAddr);

    linkBytecode(ParamChangeRegistryForwarder, forwarderTargetPlaceholder, paramChangeRegistryAddr);
    const paramChangeRegistryForwarder = await deployer.deploy(ParamChangeRegistryForwarder, Object.assign(opts, {gas: gas}));
    return {[sk.paramChangeRegistryForwarderAddr]: paramChangeRegistryForwarder.address};
  });

  await status.step(async ()=> {
    const memeRegistryDbAddr = status.getValue(sk.memeRegistryDbAddr);
    const memeRegistryForwarderAddr = status.getValue(sk.memeRegistryForwarderAddr);
    const memeRegistry = await MemeRegistry.at (memeRegistryForwarderAddr);
    await memeRegistry.construct (memeRegistryDbAddr, Object.assign(opts, {gas: 100000}));
    const db = await memeRegistry.db();
    console.log ("@@@ MemeRegistry/db :", db);
  });

  await status.step(async ()=> {
    const paramChangeRegistryDbAddr = status.getValue(sk.paramChangeRegistryDbAddr);
    const paramChangeRegistryForwarderAddr = status.getValue(sk.paramChangeRegistryForwarderAddr);

    const paramChangeRegistry = await ParamChangeRegistry.at (paramChangeRegistryForwarderAddr);
    await paramChangeRegistry.construct (paramChangeRegistryDbAddr, Object.assign(opts, {gas: 100000}));
  });

  await status.step(async ()=> {
    const paramChangeRegistryForwarderAddr = status.getValue(sk.paramChangeRegistryForwarderAddr);
    const dsGuardAddr = status.getValue(sk.dsGuardAddr);

    const dsGuard = await DSGuard.at(dsGuardAddr);
    await dsGuard.methods['permit(address,address,bytes32)'].sendTransaction(paramChangeRegistryForwarderAddr, dsGuardAddr, '0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff', Object.assign(opts, {gas: 100000}));
  });

  await status.step(async ()=> {
    const memeRegistryForwarderAddr = status.getValue(sk.memeRegistryForwarderAddr);
    const dsGuardAddr = status.getValue(sk.dsGuardAddr);

    const memeAuth = await deployer.deploy (MemeAuth, memeRegistryForwarderAddr, dsGuardAddr, Object.assign(opts, {gas: 3200000}));
    return {[sk.memeAuthAddr]: memeAuth.address};
  });

  await status.step(async ()=> {
    const memeTokenChild = await deployer.deploy (MemeTokenChild, Object.assign(opts, {gas: 5200000}));
    return {[sk.memeTokenChildAddr]: memeTokenChild.address};
  });

  await status.step(async ()=> {
    const memeAuthAddr = status.getValue(sk.memeAuthAddr);
    const memeTokenChildAddr = status.getValue(sk.memeTokenChildAddr);

    const memeTokenChild = await MemeTokenChild.at(memeTokenChildAddr);
    await memeTokenChild.setAuthority(memeAuthAddr, Object.assign(opts, {gas: 100000}));
  });

  await status.step(async ()=> {
    const dankTokenChildAddr = status.getValue(sk.dankTokenChildAddr);
    const memeRegistryForwarderAddr = status.getValue(sk.memeRegistryForwarderAddr);
    const districtConfigAddr = status.getValue(sk.districtConfigAddr);
    const memeTokenChildAddr = status.getValue(sk.memeTokenChildAddr);

    linkBytecode(Meme, dankTokenPlaceholder, dankTokenChildAddr);
    linkBytecode(Meme, registryPlaceholder, memeRegistryForwarderAddr);
    linkBytecode(Meme, districtConfigPlaceholder, districtConfigAddr);
    linkBytecode(Meme, memeTokenPlaceholder, memeTokenChildAddr);
    const meme = await deployer.deploy(Meme, Object.assign(opts, {gas: 6721975}));
    return {[sk.memeAddr]: meme.address};
  });

  await status.step(async ()=> {
    const dankTokenChildAddr = status.getValue(sk.dankTokenChildAddr);
    const paramChangeRegistryForwarderAddr = status.getValue(sk.paramChangeRegistryForwarderAddr);

    linkBytecode(ParamChange, dankTokenPlaceholder, dankTokenChildAddr);
    linkBytecode(ParamChange, registryPlaceholder, paramChangeRegistryForwarderAddr);
    const paramChange = await deployer.deploy (ParamChange, Object.assign(opts, {gas: 6000000}));
    return {[sk.paramChangeAddr]: paramChange.address};
  });

  await status.step(async ()=> {
    const memeAddr = status.getValue(sk.memeAddr);
    const memeRegistryForwarderAddr = status.getValue(sk.memeRegistryForwarderAddr);
    const dankTokenChildAddr = status.getValue(sk.dankTokenChildAddr);
    const memeTokenChildAddr = status.getValue(sk.memeTokenChildAddr);

    linkBytecode(MemeFactory, forwarderTargetPlaceholder, memeAddr);
    const memeFactory = await deployer.deploy (MemeFactory, memeRegistryForwarderAddr, dankTokenChildAddr, memeTokenChildAddr, Object.assign(opts, {gas: gas}));
    return {[sk.memeFactoryAddr]: memeFactory.address};
  });

  await status.step(async ()=> {
    const paramChangeAddr = status.getValue(sk.paramChangeAddr);
    const paramChangeRegistryForwarderAddr = status.getValue(sk.paramChangeRegistryForwarderAddr);
    const dankTokenChildAddr = status.getValue(sk.dankTokenChildAddr);

    linkBytecode(ParamChangeFactory, forwarderTargetPlaceholder, paramChangeAddr);
    const paramChangeFactory = await deployer.deploy (ParamChangeFactory, paramChangeRegistryForwarderAddr, dankTokenChildAddr, Object.assign(opts, {gas: gas}));
    return {[sk.paramChangeFactoryAddr]: paramChangeFactory.address};
  });

  await status.step(async ()=> {
    const memeRegistryDbAddr = status.getValue(sk.memeRegistryDbAddr);

    const memeRegistryDb = await MemeRegistryDb.at(memeRegistryDbAddr);
    await memeRegistryDb.setUIntValues (['challengePeriodDuration',
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
        Object.assign(opts, {gas: 500000}));

    const deposit = await memeRegistryDb.getUIntValue (web3Utils.soliditySha3 ("deposit"));
    console.log ("MemeRegistryDb/deposit:", deposit);
  });

  await status.step(async ()=> {
    const paramChangeRegistryDbAddr = status.getValue(sk.paramChangeRegistryDbAddr);

    const paramChangeRegistryDb = await ParamChangeRegistryDb.at(paramChangeRegistryDbAddr);
    await paramChangeRegistryDb.setUIntValues (['challengePeriodDuration',
          'commitPeriodDuration',
          'revealPeriodDuration',
          'deposit',
          'challengeDispensation'].map((k) => {return web3Utils.soliditySha3(k);}),
        [parameters.paramChangeRegistryDb.challengePeriodDuration,
          parameters.paramChangeRegistryDb.commitPeriodDuration,
          parameters.paramChangeRegistryDb.revealPeriodDuration ,
          parameters.paramChangeRegistryDb.deposit  ,
          parameters.paramChangeRegistryDb.challengeDispensation],
        Object.assign(opts, {gas: 500000}));
  });

  //  make :ds-guard authority of both :meme-registry-db and :param-change-registry-db
  await status.step(async ()=> {
    const dsGuardAddr = status.getValue(sk.dsGuardAddr);
    const memeRegistryDbAddr = status.getValue(sk.memeRegistryDbAddr);

    const memeRegistryDb = await MemeRegistryDb.at(memeRegistryDbAddr);
    await memeRegistryDb.setAuthority(dsGuardAddr, Object.assign(opts, {gas: 100000}));
  });

  await status.step(async ()=> {
    const dsGuardAddr = status.getValue(sk.dsGuardAddr);
    const paramChangeRegistryDbAddr = status.getValue(sk.paramChangeRegistryDbAddr);

    const paramChangeRegistryDb = await ParamChangeRegistryDb.at(paramChangeRegistryDbAddr);
    await paramChangeRegistryDb.setAuthority(dsGuardAddr, Object.assign(opts, {gas: 100000}));
  });

  // after authority is set, we can clean owner. Not really essential, but extra safety measure
  await status.step(async ()=> {
    const memeRegistryDbAddr = status.getValue(sk.memeRegistryDbAddr);

    const memeRegistryDb = await MemeRegistryDb.at(memeRegistryDbAddr);
    await memeRegistryDb.setOwner("0x0000000000000000000000000000000000000000", Object.assign(opts, {gas: 100000}));
  });

  await status.step(async ()=> {
    const paramChangeRegistryDbAddr = status.getValue(sk.paramChangeRegistryDbAddr);

    const paramChangeRegistryDb = await ParamChangeRegistryDb.at(paramChangeRegistryDbAddr);
    await paramChangeRegistryDb.setOwner("0x0000000000000000000000000000000000000000", Object.assign(opts, {gas: 100000}));
  });

  // allow :param-change-registry-fwd to make changes into :meme-registry-db (to apply ParamChanges)
  await status.step(async ()=> {
    const dsGuardAddr = status.getValue(sk.dsGuardAddr);
    const memeRegistryForwarderAddr = status.getValue(sk.memeRegistryForwarderAddr);
    const memeRegistryDbAddr = status.getValue(sk.memeRegistryDbAddr);

    const dsGuard = await DSGuard.at(dsGuardAddr);
    await dsGuard.methods['permit(address,address,bytes32)'].sendTransaction(memeRegistryForwarderAddr, memeRegistryDbAddr, '0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff', Object.assign(opts, {gas: 100000}));
  });

  await status.step(async ()=> {
    const dsGuardAddr = status.getValue(sk.dsGuardAddr);
    const paramChangeRegistryForwarderAddr = status.getValue(sk.paramChangeRegistryForwarderAddr);
    const memeRegistryDbAddr = status.getValue(sk.memeRegistryDbAddr);

    const dsGuard = await DSGuard.at(dsGuardAddr);
    await dsGuard.methods['permit(address,address,bytes32)'].sendTransaction(paramChangeRegistryForwarderAddr, memeRegistryDbAddr, '0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff', Object.assign(opts, {gas: 100000}));
  });

  // allow :param-change-registry-fwd to make changes into :param-change-registry-db
  await status.step(async ()=> {
    const dsGuardAddr = status.getValue(sk.dsGuardAddr);
    const paramChangeRegistryForwarderAddr = status.getValue(sk.paramChangeRegistryForwarderAddr);
    const paramChangeRegistryDbAddr = status.getValue(sk.paramChangeRegistryDbAddr);

    const dsGuard = await DSGuard.at(dsGuardAddr);
    await dsGuard.methods['permit(address,address,bytes32)'].sendTransaction(paramChangeRegistryForwarderAddr, paramChangeRegistryDbAddr, '0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff', Object.assign(opts, {gas: 100000}));
  });


  await status.step(async ()=> {
    const memeRegistryForwarderAddr = status.getValue(sk.memeRegistryForwarderAddr);
    const memeFactoryAddr = status.getValue(sk.memeFactoryAddr);

    const memeRegistry = await MemeRegistry.at(memeRegistryForwarderAddr);
    await memeRegistry.setFactory (memeFactoryAddr, true, Object.assign(opts, {gas: 100000}));
    const isFactory = await memeRegistry.isFactory (memeFactoryAddr);
    console.log ("@@@ MemeRegistry/isFactory", isFactory);
  });

  await status.step(async ()=> {
    const paramChangeRegistryForwarderAddr = status.getValue(sk.paramChangeRegistryForwarderAddr);
    const paramChangeFactoryAddr = status.getValue(sk.paramChangeFactoryAddr);

    const paramChangeRegistry = await ParamChangeRegistry.at (paramChangeRegistryForwarderAddr);
    await paramChangeRegistry.setFactory (paramChangeFactoryAddr, true, Object.assign(opts, {gas: 100000}));
  });

  await status.step(async ()=> {
    const memeAuctionFactoryForwarder = await deployer.deploy (MemeAuctionFactoryForwarder, Object.assign(opts, {gas: gas}));

    return {[sk.memeAuctionFactoryForwarderAddr]: memeAuctionFactoryForwarder.address}
  });

  await status.step(async ()=> {
    const dsGuardAddr = status.getValue(sk.dsGuardAddr);
    const memeAuctionFactoryForwarderAddr = status.getValue(sk.memeAuctionFactoryForwarderAddr);

    const memeAuctionFactoryForwarder = await MemeAuctionFactoryForwarder.at(memeAuctionFactoryForwarderAddr);
    await memeAuctionFactoryForwarder.setAuthority(dsGuardAddr, Object.assign(opts, {gas: 100000}));
  });

  await status.step(async ()=> {
    const memeAuctionFactoryForwarderAddr = status.getValue(sk.memeAuctionFactoryForwarderAddr);
    const memeRegistryForwarderAddr = status.getValue(sk.memeRegistryForwarderAddr);
    const districtConfigAddr = status.getValue(sk.districtConfigAddr);
    const memeTokenChildAddr = status.getValue(sk.memeTokenChildAddr);

    linkBytecode(MemeAuction, memeAuctionFactoryPlaceholder, memeAuctionFactoryForwarderAddr);
    linkBytecode(MemeAuction, registryPlaceholder, memeRegistryForwarderAddr);
    linkBytecode(MemeAuction, districtConfigPlaceholder, districtConfigAddr);
    linkBytecode(MemeAuction, memeTokenPlaceholder, memeTokenChildAddr);
    const memeAuction = await deployer.deploy(MemeAuction, Object.assign(opts, {gas: 4000000}));
    return {[sk.memeAuctionAddr]: memeAuction.address};
  });

  await status.step(async ()=> {
    const memeAuctionAddr = status.getValue(sk.memeAuctionAddr);

    linkBytecode(MemeAuctionFactory, forwarderTargetPlaceholder, memeAuctionAddr);
    const memeAuctionFactory = await deployer.deploy(MemeAuctionFactory, Object.assign(opts, {gas: 2000000}));
    return {[sk.memeAuctionFactoryAddr]: memeAuctionFactory.address};
  });

  await status.step(async ()=> {
    const memeAuctionFactoryAddr = status.getValue(sk.memeAuctionFactoryAddr);
    const memeAuctionFactoryForwarderAddr = status.getValue(sk.memeAuctionFactoryForwarderAddr);

    const memeAuctionFactoryForwarder = await MemeAuctionFactoryForwarder.at(memeAuctionFactoryForwarderAddr);
    await memeAuctionFactoryForwarder.setTarget(memeAuctionFactoryAddr, Object.assign(opts, {gas: 100000}));
  });

  await status.step(async ()=> {
    const memeTokenChildAddr = status.getValue(sk.memeTokenChildAddr);
    const memeAuctionFactoryForwarderAddr = status.getValue(sk.memeAuctionFactoryForwarderAddr);

    const memeAuctionFactory = await MemeAuctionFactory.at (memeAuctionFactoryForwarderAddr)
    await memeAuctionFactory.construct (memeTokenChildAddr, Object.assign(opts, {gas: 200000}));
  });

  await status.step(async ()=> {
    const dankTokenChildAddr = status.getValue(sk.dankTokenChildAddr);

    const dankChildController = await deployer.deploy(DankChildController, dankTokenChildAddr, Object.assign(opts, {gas: 4000000}));
    return {[sk.dankChildControllerAddr]: dankChildController.address};
  });

  await status.step(async ()=> {
    const dankChildControllerAddr = status.getValue(sk.dankChildControllerAddr);
    const dankTokenChildAddr = status.getValue(sk.dankTokenChildAddr);

    const dankTokenChild = await DankTokenChild.at(dankTokenChildAddr);
    await dankTokenChild.changeController(dankChildControllerAddr, Object.assign(opts, {gas: 100000}));
  });

  await status.step(async ()=> {
    const dankChildControllerAddr = status.getValue(sk.dankChildControllerAddr);
    const dsGuardAddr = status.getValue(sk.dsGuardAddr);

    const dankChildController = await DankChildController.at(dankChildControllerAddr);
    await dankChildController.setAuthority(dsGuardAddr, Object.assign(opts, {gas: 200000, value: 0}));
    const authority = await dankChildController.authority ();
    console.log ("@@@ DankChildController authority: ", authority);
  });

  var smartContracts = edn.encode(
   new edn.Map([

     edn.kw(":migrations"), new edn.Map([edn.kw(":name"), "Migrations",
                                         edn.kw(":address"), migrations.address]),

     edn.kw(":district-config"), new edn.Map([edn.kw(":name"), "DistrictConfig",
                                              edn.kw(":address"), status.getValue(sk.districtConfigAddr)]),

     edn.kw(":ds-guard"), new edn.Map([edn.kw(":name"), "DSGuard",
                                       edn.kw(":address"), status.getValue(sk.dsGuardAddr)]),

     edn.kw(":ds-guard-root"), new edn.Map([edn.kw(":name"), "DSGuard",
                                       edn.kw(":address"), dsGuardRoot]),

     edn.kw(":meme-auth"), new edn.Map([edn.kw(":name"), "MemeAuth",
                                       edn.kw(":address"), status.getValue(sk.memeAuthAddr)]),

     edn.kw(":param-change-registry"), new edn.Map([edn.kw(":name"), "ParamChangeRegistry",
                                                    edn.kw(":address"), status.getValue(sk.paramChangeRegistryAddr)]),

     edn.kw(":param-change-registry-db"), new edn.Map([edn.kw(":name"), "EternalDb",
                                                       edn.kw(":address"), status.getValue(sk.paramChangeRegistryDbAddr)]),

     edn.kw(":meme-registry-db"), new edn.Map([edn.kw(":name"), "EternalDb",
                                               edn.kw(":address"), status.getValue(sk.memeRegistryDbAddr)]),

     edn.kw(":param-change"), new edn.Map([edn.kw(":name"), "ParamChange",
                                           edn.kw(":address"), status.getValue(sk.paramChangeAddr)]),

     edn.kw(":minime-token-factory"), new edn.Map([edn.kw(":name"), "MiniMeTokenFactory",
                                                   edn.kw(":address"), status.getValue(sk.miniMeTokenFactoryAddr)]),

     edn.kw(":meme-auction-factory"), new edn.Map([edn.kw(":name"), "MemeAuctionFactory",
                                                   edn.kw(":address"), status.getValue(sk.memeAuctionFactoryAddr)]),

     edn.kw(":meme-auction"), new edn.Map([edn.kw(":name"), "MemeAuction",
                                           edn.kw(":address"), status.getValue(sk.memeAuctionAddr)]),

     edn.kw(":param-change-factory"), new edn.Map([edn.kw(":name"), "ParamChangeFactory",
                                                   edn.kw(":address"), status.getValue(sk.paramChangeFactoryAddr)]),

     edn.kw(":param-change-registry-fwd"), new edn.Map([edn.kw(":name"), "MutableForwarder",
                                                        edn.kw(":address"), status.getValue(sk.paramChangeRegistryForwarderAddr),
                                                        edn.kw(":forwards-to"), edn.kw(":param-change-registry")]),

     edn.kw(":meme-factory"), new edn.Map([edn.kw(":name"), "MemeFactory",
                                           edn.kw(":address"), status.getValue(sk.memeFactoryAddr)]),

     edn.kw(":meme-token-root"), new edn.Map([edn.kw(":name"), "MemeToken",
                                         edn.kw(":address"), "0x0000000000000000000000000000000000000000"]),

     edn.kw(":meme-token"), new edn.Map([edn.kw(":name"), "MemeTokenChild",
                                         edn.kw(":address"), status.getValue(sk.memeTokenChildAddr)]),

     edn.kw(":DANK"), new edn.Map([edn.kw(":name"), "DankTokenChild",
                                   edn.kw(":address"), status.getValue(sk.dankTokenChildAddr)]),

     edn.kw(":DANK-root"), new edn.Map([edn.kw(":name"), "DankToken",
                                   edn.kw(":address"), dankRoot]),

     edn.kw(":meme-registry"), new edn.Map([edn.kw(":name"), "Registry",
                                            edn.kw(":address"), status.getValue(sk.memeRegistryAddr)]),

     edn.kw(":meme"), new edn.Map([edn.kw(":name"), "Meme",
                                   edn.kw(":address"), status.getValue(sk.memeAddr)]),

     edn.kw(":meme-registry-fwd"), new edn.Map([edn.kw(":name"), "MutableForwarder",
                                                edn.kw(":address"), status.getValue(sk.memeRegistryForwarderAddr),
                                                edn.kw(":forwards-to"), edn.kw(":meme-registry")]),

     edn.kw(":meme-auction-factory-fwd"), new edn.Map([edn.kw(":name"), "MutableForwarder",
                                                       edn.kw(":address"), status.getValue(sk.memeAuctionFactoryForwarderAddr),
                                                       edn.kw(":forwards-to"), edn.kw(":meme-auction-factory")]),

     edn.kw(":district0x-emails"), new edn.Map([edn.kw(":name"), "District0xEmails",
                                                edn.kw(":address"), status.getValue(sk.district0xEmailsAddr)]),

     edn.kw(":ens"), new edn.Map([edn.kw(":name"), "ENS",
                                                edn.kw(":address"), parameters.ENS]),

     edn.kw(":DANK-child-controller"), new edn.Map([edn.kw(":name"), "DankChildController",
                                          edn.kw(":address"), status.getValue(sk.dankChildControllerAddr)]),

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

  status.clean();
  console.log ("Done");

}