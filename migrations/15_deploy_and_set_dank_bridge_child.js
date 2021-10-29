// Execute this script on L2 (polygon)

const {readSmartContractsFile, getSmartContractAddress, setSmartContractAddress, writeSmartContracts, Status} = require ("../migrations/utils.js");
const {parameters, smart_contracts_path, env} = require ('../truffle.js');

const smartContracts = readSmartContractsFile(smart_contracts_path);
const fxChild = parameters.fxChild;
const dankChildController = getSmartContractAddress(smartContracts, ":DANK-child-controller");
const tunnelRoot = getSmartContractAddress(smartContracts, ":DANK-root-tunnel");

const DankChildTunnel = artifacts.require('DankChildTunnel');

module.exports = async(deployer, network, accounts) => {
    const address = accounts [0];
    const gas = 4e6;
    const opts = {gas: gas, from: address};

    await deployer;

    let status = new Status("15");

    console.log('Deploying DankChildTunnel');
    console.log(`FxChild:${fxChild}, DankChildController:${dankChildController}, DankRootTunnel:${tunnelRoot}`);

    if (!fxChild || !dankChildController || !tunnelRoot)
        throw new Error("Missing required contract address");

    await status.step(async ()=> {
        const tunnelChild = await deployer.deploy(DankChildTunnel, fxChild, dankChildController, Object.assign(opts, {gas: 4000000}));
        return {tunnelChild: tunnelChild.address};
    });

    await status.step(async ()=> {
        const tunnelChildAddr = status.getValue('tunnelChild');
        const tunnelChild = await DankChildTunnel.at(tunnelChildAddr);
        console.log('Setting up DankChildTunnel. TunnelRoot:' + tunnelRoot);
        await tunnelChild.setFxRootTunnel(tunnelRoot);
    });

     setSmartContractAddress(smartContracts, ":DANK-child-tunnel", status.getValue('tunnelChild'));

     writeSmartContracts(smart_contracts_path, smartContracts, env);

     status.clean();

}
