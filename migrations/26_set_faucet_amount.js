// Execute this script on L2 (polygon)
require('web3');
const {readSmartContractsFile, getSmartContractAddress} = require ("../migrations/utils.js");
const {smart_contracts_path} = require ('../truffle.js');
const BigNumber = require("bignumber.js");

const smartContracts = readSmartContractsFile(smart_contracts_path);
const faucetAddr = getSmartContractAddress(smartContracts, ":dank-faucet");

const DankFaucet = artifacts.require('DankFaucet');


module.exports = async(deployer, network, accounts) => {
    const address = accounts [0];
    const gas = 4e6;
    const opts = {gas: gas, from: address};

    await deployer;

    console.log('Setting DANK amount to Faucet');
    console.log(`DankFaucet:${faucetAddr}`);

    if (!faucetAddr)
        throw new Error("Missing required contract address");

    const faucet = await DankFaucet.at(faucetAddr);
    await faucet.resetAllotment(new BigNumber("100e18"), Object.assign(opts, {gas: 100000}));

}
