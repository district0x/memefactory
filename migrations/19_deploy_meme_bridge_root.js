// Execute this script on L1 (eth)

const {readSmartContractsFile, getSmartContractAddress, setSmartContractAddress, writeSmartContracts} = require ("../migrations/utils.js");
const {parameters, smart_contracts_path, env} = require ('../truffle.js');

const smartContracts = readSmartContractsFile(smart_contracts_path);
const checkpointManager = parameters.polygonCheckpointManager;
const fxRoot = parameters.fxRoot;
const memeTokenRoot = getSmartContractAddress(smartContracts, ":meme-token-root");

const MemeTokenRootTunnel = artifacts.require('MemeTokenRootTunnel');

module.exports = async(deployer, network, accounts) => {
    const address = accounts [0];
    const gas = 4e6;
    const opts = {gas: gas, from: address};

    await deployer;

    console.log('Deploying MemeTokenRootTunnel');
    console.log(`CheckpointManager:${checkpointManager}, FxRoot:${fxRoot}, MemeTokenRoot:${memeTokenRoot}`);

    if (!checkpointManager || !fxRoot || !memeTokenRoot)
        throw new Error("Missing required contract address");

    const tunnelRoot = await deployer.deploy(MemeTokenRootTunnel, checkpointManager, fxRoot, memeTokenRoot, Object.assign(opts, {gas: 4000000}));

    setSmartContractAddress(smartContracts, ":meme-token-root-tunnel", tunnelRoot.address);

    writeSmartContracts(smart_contracts_path, smartContracts, env);
}
