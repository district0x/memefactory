// Execute this script on L1 (eth)

const {readSmartContractsFile, getSmartContractAddress, setSmartContractAddress, writeSmartContracts} = require ("../migrations/utils.js");
const {parameters, smart_contracts_path, env} = require ('../truffle.js');

const smartContracts = readSmartContractsFile(smart_contracts_path);
const checkpointManager = parameters.polygonCheckpointManager;
const fxRoot = parameters.fxRoot;
const dankRoot = getSmartContractAddress(smartContracts, ":DANK-root");

const DankRootTunnel = artifacts.require('DankRootTunnel');

module.exports = async(deployer, network, accounts) => {
    const address = accounts [0];
    const gas = 4e6;
    const opts = {gas: gas, from: address};

    await deployer;

    console.log('Deploying DankRootTunnel');
    console.log(`CheckpointManager:${checkpointManager}, FxRoot:${fxRoot}, DankRoot:${dankRoot}`);

    if (!checkpointManager || !fxRoot || !dankRoot)
        throw new Error("Missing required contract address");

    const tunnelRoot = await deployer.deploy(DankRootTunnel, checkpointManager, fxRoot, dankRoot, Object.assign(opts, {gas: 4000000}));

    setSmartContractAddress(smartContracts, ":DANK-root-tunnel", tunnelRoot.address);

    writeSmartContracts(smart_contracts_path, smartContracts, env);
}
