// Execute this script on L2 (polygon)
// IMPORTANT. This is only for testing
require('web3');
const {readSmartContractsFile, getSmartContractAddress} = require ("../migrations/utils.js");
const {smart_contracts_path} = require ('../truffle.js');
const {Status, setSmartContractAddress, writeSmartContracts, linkBytecode} = require("./utils");
const {parameters, env} = require("../truffle");

const smartContracts = readSmartContractsFile(smart_contracts_path);
const dsGuardAddr = getSmartContractAddress(smartContracts, ":ds-guard");
const dankTokenChildAddr = getSmartContractAddress(smartContracts, ":DANK");

const DSGuard = artifacts.require('DSGuard');
const DankFaucet = artifacts.require('DankFaucet');

const dankTokenPlaceholder = "deaddeaddeaddeaddeaddeaddeaddeaddeaddead";

module.exports = async(deployer, network, accounts) => {
    const address = accounts [0];
    const gas = 4e6;
    const opts = {gas: gas, from: address};

    await deployer;

    console.log('Deploy Faucet');
    console.log(`DSGuard:${dsGuardAddr}, DankTokenChild:${dankTokenChildAddr}`);

    if (!dsGuardAddr || !dankTokenChildAddr)
        throw new Error("Missing required contract address");

    let status = new Status("25");

    await status.step(async ()=> {
        linkBytecode(DankFaucet, dankTokenPlaceholder, dankTokenChildAddr);
        const dankFaucet = await deployer.deploy (DankFaucet, parameters.dankFaucet.allotment, Object.assign(opts, {gas: 1000000}));
        return {dankFaucet: dankFaucet.address};
    });

    await status.step(async ()=> {
        const dankFaucetAddr = status.getValue('dankFaucet');
        const dankFaucet = await DankFaucet.at(dankFaucetAddr);

        await dankFaucet.setAuthority(dsGuardAddr, Object.assign(opts, {gas: 100000}));
    });

    await status.step(async ()=> {
        const dankFaucetAddr = status.getValue('dankFaucet');
        const dsGuard = await DSGuard.at(dsGuardAddr);
        await dsGuard.methods['permit(address,address,bytes32)'].sendTransaction(address, dankFaucetAddr, "0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", Object.assign(opts, {gas: 100000}));
    });

    await status.step(async ()=> {
        const dankFaucetAddr = status.getValue('dankFaucet');
        const dsGuard = await DSGuard.at(dsGuardAddr);
        const sigSendDank = web3.utils.sha3('sendDank(bytes32,address)').substring(0, 10);

        await dsGuard.methods['permit(address,address,bytes32)'].sendTransaction(parameters.dankFaucet.sender, dankFaucetAddr, sigSendDank, Object.assign(opts, {gas: 100000}));
    });

    setSmartContractAddress(smartContracts, ":dank-faucet", status.getValue('dankFaucet'));

    writeSmartContracts(smart_contracts_path, smartContracts, env);

    status.clean();

}
