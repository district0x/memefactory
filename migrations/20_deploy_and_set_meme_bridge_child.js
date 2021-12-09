// Execute this script on L2 (polygon)

const {readSmartContractsFile, getSmartContractAddress, setSmartContractAddress, writeSmartContracts, Status} = require ("../migrations/utils.js");
const {parameters, smart_contracts_path, env} = require ('../truffle.js');

const smartContracts = readSmartContractsFile(smart_contracts_path);
const fxChild = parameters.fxChild;
const memeTokenChild = getSmartContractAddress(smartContracts, ":meme-token");
const tunnelRoot = getSmartContractAddress(smartContracts, ":meme-token-root-tunnel");

const ERC1967Proxy = artifacts.require("ERC1967Proxy");
const MemeTokenChildTunnel = artifacts.require('MemeTokenChildTunnel');

module.exports = async(deployer, network, accounts) => {
    const address = accounts [0];
    const gas = 4e6;
    const opts = {gas: gas, from: address};

    await deployer;

    let status = new Status("20");

    console.log('Deploying MemeTokenChildTunnel');
    console.log(`FxChild:${fxChild}, MemeTokenChild:${memeTokenChild}, MemeTokenRootTunnel:${tunnelRoot}`);

    if (!fxChild || !memeTokenChild || !tunnelRoot)
        throw new Error("Missing required contract address");

    await status.step(async ()=> {
        console.log('Deploying tunnelChild implementation');
        const tunnelChild = await deployer.deploy(MemeTokenChildTunnel, Object.assign(opts, {gas: 4000000}));
        return {tunnelChild: tunnelChild.address};
    });

    await status.step(async ()=> {
        console.log('Deploying tunnelChild proxy');
        const tunnelChildAddr = status.getValue('tunnelChild');
        const tunnelChild = await MemeTokenChildTunnel.at(tunnelChildAddr);
        const data = tunnelChild.contract.methods.initialize(fxChild, memeTokenChild).encodeABI();
        const tunnelChildProxy = await deployer.deploy(ERC1967Proxy, tunnelChildAddr, data, Object.assign(opts, {gas: 4000000}));
        return {tunnelChildProxy: tunnelChildProxy.address};
    });

    await status.step(async ()=> {
        const tunnelChildProxyAddr = status.getValue('tunnelChildProxy');
        const tunnelChild = await MemeTokenChildTunnel.at(tunnelChildProxyAddr);
        console.log('Setting up MemeTokenChildTunnel. TunnelRoot:'+tunnelRoot);
        await tunnelChild.setFxRootTunnel(tunnelRoot, Object.assign(opts, {gas: 400000}));
    });

    setSmartContractAddress(smartContracts, ":meme-token-child-tunnel", status.getValue('tunnelChildProxy'));

    writeSmartContracts(smart_contracts_path, smartContracts, env);

    status.clean();
}
