// Execute this script on L1 (eth)

const {readSmartContractsFile, getSmartContractAddress} = require ("../migrations/utils.js");
const {smart_contracts_path} = require ('../truffle.js');

const smartContracts = readSmartContractsFile(smart_contracts_path);
const tunnelRootAddr = getSmartContractAddress(smartContracts, ":meme-token-root-tunnel");
const tunnelChild = getSmartContractAddress(smartContracts, ":meme-token-child-tunnel");

const MemeTokenRootTunnel = artifacts.require('MemeTokenRootTunnel');

module.exports = async(deployer, network, accounts) => {
    const address = accounts [0];
    const gas = 4e6;
    const opts = {gas: gas, from: address};

    await deployer;

    console.log(`Setting up MemeTokenRootTunnel. TunnelRoot: ${tunnelRootAddr}, TunnelChild:${tunnelChild}`);

    if (!tunnelRootAddr || !tunnelChild)
        throw new Error("Missing required contract address");

    const tunnelRoot = await MemeTokenRootTunnel.at(tunnelRootAddr);

    await tunnelRoot.setFxChildTunnel(tunnelChild, opts);
}
