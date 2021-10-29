// Execute this script on L2 (polygon)
require('web3');
const {readSmartContractsFile, getSmartContractAddress} = require ("../migrations/utils.js");
const {smart_contracts_path} = require ('../truffle.js');

const smartContracts = readSmartContractsFile(smart_contracts_path);
const tunnelChildAddr = getSmartContractAddress(smartContracts, ":meme-token-child-tunnel");
const memeTokenChildAddr = getSmartContractAddress(smartContracts, ":meme-token");
const dsGuardAddr = getSmartContractAddress(smartContracts, ":ds-guard");

const DSGuard = artifacts.require('DSGuard');

module.exports = async(deployer, network, accounts) => {
    const address = accounts [0];
    const gas = 4e6;
    const opts = {gas: gas, from: address};

    await deployer;

    console.log('Allowing MemeTokenChildTunnel to mint/burn tokens');
    console.log(`MemeTokenChild:${memeTokenChildAddr}, DSGuard:${dsGuardAddr}, TunnelChild:${tunnelChildAddr}`);

    if (!dsGuardAddr || !memeTokenChildAddr || !tunnelChildAddr)
        throw new Error("Missing required contract address");

    const dsGuard = await DSGuard.at(dsGuardAddr);

    const sigMint = web3.utils.sha3('mint(address,uint256)').substring(0, 10);
    const sigMintWithData = web3.utils.sha3('mint(address,uint256,bytes)').substring(0, 10);
    const sigBurn = web3.utils.sha3('burn(uint256)').substring(0, 10);

    await dsGuard.methods['permit(address,address,bytes32)'].sendTransaction(tunnelChildAddr, memeTokenChildAddr, sigMint, Object.assign(opts, {gas: 100000}));
    await dsGuard.methods['permit(address,address,bytes32)'].sendTransaction(tunnelChildAddr, memeTokenChildAddr, sigMintWithData, Object.assign(opts, {gas: 100000}));
    await dsGuard.methods['permit(address,address,bytes32)'].sendTransaction(tunnelChildAddr, memeTokenChildAddr, sigBurn, Object.assign(opts, {gas: 100000}));

}
