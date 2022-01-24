// Execute this script on L2 (polygon)
// IMPORTANT. This is only for testing
require('web3');
const {readSmartContractsFile, getSmartContractAddress} = require ("../migrations/utils.js");
const {smart_contracts_path} = require ('../truffle.js');
const web3Utils = require("web3-utils");
const BigNumber = require("bignumber.js");

const smartContracts = readSmartContractsFile(smart_contracts_path);
const dsGuardAddr = getSmartContractAddress(smartContracts, ":ds-guard");
const paramChangeRegistryDbAddr = getSmartContractAddress(smartContracts, ":param-change-registry-db");

const DSGuard = artifacts.require('DSGuard');
const EternalDB = artifacts.require('EternalDb');

module.exports = async(deployer, network, accounts) => {
    const address = accounts [0];
    const gas = 4e6;
    const opts = {gas: gas, from: address};

    await deployer;

    console.log('Setting Parameter Deposit to 1 DANK');
    console.log(`paramChangeRegistryDbAddr:${paramChangeRegistryDbAddr}, DSGuard:${dsGuardAddr}`);

    if (!dsGuardAddr || !paramChangeRegistryDbAddr)
        throw new Error("Missing required contract address");

    const dsGuard = await DSGuard.at(dsGuardAddr);
    await dsGuard.methods['permit(address,address,bytes32)'].sendTransaction(address, paramChangeRegistryDbAddr, "0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", Object.assign(opts, {gas: 100000}));

    const paramChangeRegistryDb = await EternalDB.at(paramChangeRegistryDbAddr);
    await paramChangeRegistryDb.setUIntValues (['deposit'].map((k) => {return web3Utils.soliditySha3(k);}),
      [new BigNumber("1e18")],
      Object.assign(opts, {gas: 100000}));

}
