// Execute this script on L1 (eth)

const {readSmartContractsFile, getSmartContractAddress} = require ("../migrations/utils.js");
const {smart_contracts_path} = require ('../truffle.js');

const smartContracts = readSmartContractsFile(smart_contracts_path);
const tunnelRootAddr = getSmartContractAddress(smartContracts, ":DANK-root-tunnel");
const tunnelChild = getSmartContractAddress(smartContracts, ":DANK-child-tunnel");

const DankRootTunnel = artifacts.require('DankRootTunnel');

module.exports = async(deployer, network, accounts) => {
    const address = accounts [0];
    const gas = 4e6;
    const opts = {gas: gas, from: address};

    await deployer;

    console.log(`Setting up DankRootTunnel. TunnelRoot: ${tunnelRootAddr}, TunnelChild:${tunnelChild}`);

    if (!tunnelRootAddr || !tunnelChild)
        throw new Error("Missing required contract address");

    const tunnelRoot = await DankRootTunnel.at(tunnelRootAddr);

    await tunnelRoot.setFxChildTunnel(tunnelChild, opts);
}
