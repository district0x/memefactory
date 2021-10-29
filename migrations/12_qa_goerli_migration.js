// Deployes a dank token in testnet to check polygon bridges later on

const {setSmartContractAddress, writeSmartContracts} = require ("../migrations/utils.js");
const {smart_contracts_path, env} = require ('../truffle.js');
const {readSmartContractsFile} = require("./utils.js");
const smartContracts = readSmartContractsFile(smart_contracts_path);

const MiniMeTokenFactory = artifacts.require('MiniMeTokenFactory');
const DankToken = artifacts.require('DankToken');
const DSGuard = artifacts.require('DSGuard');

module.exports = async(deployer, network, accounts) => {
    const address = accounts [0];
    const gas = 4e6;
    const opts = {gas: gas, from: address};

    await deployer;

    console.log('Deploying DankTokenFactory');
    const dankTokenFactory = await deployer.deploy(MiniMeTokenFactory, Object.assign(opts, {gas: 2000000}));

    console.log('Deploying DankToken');
    const tunnelRoot = await deployer.deploy(DankToken, dankTokenFactory.address, "1000000000000000000000000000", Object.assign(opts, {gas: 2000000}));

    console.log('Deploying DSGuard');
    const dsGuard = await deployer.deploy(DSGuard, Object.assign(opts, {gas: 2000000}));

    setSmartContractAddress(smartContracts, ":DANK", tunnelRoot.address);
    setSmartContractAddress(smartContracts, ":ds-guard", dsGuard.address);

    writeSmartContracts(smart_contracts_path, smartContracts, env);
}
