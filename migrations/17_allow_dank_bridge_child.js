// Execute this script on L2 (polygon)
require('web3');
const {readSmartContractsFile, getSmartContractAddress} = require ("../migrations/utils.js");
const {smart_contracts_path, env} = require ('../truffle.js');

const smartContracts = readSmartContractsFile(smart_contracts_path);
const tunnelChildAddr = getSmartContractAddress(smartContracts, ":DANK-child-tunnel");
const dankChildControllerAddr = getSmartContractAddress(smartContracts, ":DANK-child-controller");
const dsGuardAddr = getSmartContractAddress(smartContracts, ":ds-guard");

const DSGuard = artifacts.require('DSGuard');

module.exports = async(deployer, network, accounts) => {
    const address = accounts [0];
    const gas = 4e6;
    const opts = {gas: gas, from: address};

    await deployer;

    console.log('Allowing DankChildTunnel to mint/burn tokens');
    console.log(`DankChildController:${dankChildControllerAddr}, DSGuard:${dsGuardAddr}`);

    if (!dsGuardAddr || !dankChildControllerAddr)
        throw new Error("Missing required contract address");

    const dsGuard = await DSGuard.at(dsGuardAddr);

    const sigMint = web3.utils.sha3('mint(address,uint256)').substring(0, 10);
    const sigBurn = web3.utils.sha3('burn(address,uint256)').substring(0, 10);

    await dsGuard.methods['permit(address,address,bytes32)'].sendTransaction(tunnelChildAddr, dankChildControllerAddr, sigMint, Object.assign(opts, {gas: 100000}));
    await dsGuard.methods['permit(address,address,bytes32)'].sendTransaction(tunnelChildAddr, dankChildControllerAddr, sigBurn, Object.assign(opts, {gas: 100000}));

}
