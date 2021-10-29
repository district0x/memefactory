// Execute this script on L1
require('web3');
const {readSmartContractsFile, getSmartContractAddress} = require ("../migrations/utils.js");
const {smart_contracts_path} = require ('../truffle.js');

const smartContracts = readSmartContractsFile(smart_contracts_path);
const tunnelRootAddr = getSmartContractAddress(smartContracts, ":meme-token-root-tunnel");
const memeTokenRootAddr = getSmartContractAddress(smartContracts, ":meme-token-root");
const dsGuardAddr = getSmartContractAddress(smartContracts, ":ds-guard-root");

const DSGuard = artifacts.require('DSGuard');

module.exports = async(deployer, network, accounts) => {
    const address = accounts [0];
    const gas = 4e6;
    const opts = {gas: gas, from: address};

    await deployer;

    console.log('Allowing MemeTokenRootTunnel to mint tokens');
    console.log(`MemeTokenRoot:${memeTokenRootAddr}, DSGuard:${dsGuardAddr}, TunnelRoot:${tunnelRootAddr}`);

    if (!dsGuardAddr || !memeTokenRootAddr || !tunnelRootAddr)
        throw new Error("Missing required contract address");

    const dsGuard = await DSGuard.at(dsGuardAddr);

    const sigMint = web3.utils.sha3('mint(address,uint256)').substring(0, 10);
    const sigMintWithData = web3.utils.sha3('mint(address,uint256,bytes)').substring(0, 10);

    await dsGuard.methods['permit(address,address,bytes32)'].sendTransaction(tunnelRootAddr, memeTokenRootAddr, sigMint, Object.assign(opts, {gas: 100000}));
    await dsGuard.methods['permit(address,address,bytes32)'].sendTransaction(tunnelRootAddr, memeTokenRootAddr, sigMintWithData, Object.assign(opts, {gas: 100000}));

}
