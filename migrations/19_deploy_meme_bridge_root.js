// Execute this script on L1 (eth)

const {readSmartContractsFile, getSmartContractAddress, setSmartContractAddress, writeSmartContracts, Status} = require ("../migrations/utils.js");
const {parameters, smart_contracts_path, env} = require ('../truffle.js');

const smartContracts = readSmartContractsFile(smart_contracts_path);
const checkpointManager = parameters.polygonCheckpointManager;
const fxRoot = parameters.fxRoot;
const memeTokenRoot = getSmartContractAddress(smartContracts, ":meme-token-root");

const ERC1967Proxy = artifacts.require("ERC1967Proxy");
const MemeTokenRootTunnel = artifacts.require('MemeTokenRootTunnel');

module.exports = async(deployer, network, accounts) => {
    const address = accounts [0];
    const gas = 4e6;
    const opts = {gas: gas, from: address};

    await deployer;

    let status = new Status("19");

    console.log('Deploying MemeTokenRootTunnel');
    console.log(`CheckpointManager:${checkpointManager}, FxRoot:${fxRoot}, MemeTokenRoot:${memeTokenRoot}`);

    if (!checkpointManager || !fxRoot || !memeTokenRoot)
        throw new Error("Missing required contract address");

    await status.step(async ()=> {
        console.log('Deploying tunnelRoot implementation');
        const tunnelRoot = await deployer.deploy(MemeTokenRootTunnel, Object.assign(opts, {gas: 4000000}));
        return {tunnelRoot: tunnelRoot.address};
    });

    await status.step(async ()=> {
        console.log('Deploying tunnelRoot proxy');
        const tunnelRootAddr = status.getValue('tunnelRoot');
        const tunnelRoot = await MemeTokenRootTunnel.at(tunnelRootAddr);
        const data = tunnelRoot.contract.methods.initialize(checkpointManager, fxRoot, memeTokenRoot).encodeABI();
        const tunnelRootProxy = await deployer.deploy(ERC1967Proxy, tunnelRootAddr, data, Object.assign(opts, {gas: 4000000}));
        return {tunnelRootProxy: tunnelRootProxy.address};
    });

    setSmartContractAddress(smartContracts, ":meme-token-root-tunnel", status.getValue('tunnelRootProxy'));

    writeSmartContracts(smart_contracts_path, smartContracts, env);

    status.clean();
}
