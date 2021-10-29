// Execute this script on L1 (eth)

const {readSmartContractsFile, getSmartContractAddress, setSmartContractAddress, writeSmartContracts, Status} = require ("../migrations/utils.js");
const {smart_contracts_path, env} = require ('../truffle.js');

const smartContracts = readSmartContractsFile(smart_contracts_path);

const dsGuardAddr = getSmartContractAddress(smartContracts, ":ds-guard-root");

const MemeToken = artifacts.require('MemeToken');

module.exports = async(deployer, network, accounts) => {
    const address = accounts [0];
    const gas = 4e6;
    const opts = {gas: gas, from: address};

    await deployer;

    let status = new Status("18");

    console.log('Deploying MemeToken in L1');
    console.log(`dsGuard:${dsGuardAddr}`);

    if (!dsGuardAddr)
        throw new Error("Missing required contract address");

    await status.step(async ()=> {
        const memeToken = await deployer.deploy(MemeToken, Object.assign(opts, {gas: 4000000}));
        return {memeToken: memeToken.address};
    });

    await status.step(async ()=> {
        const memeTokenAddr = status.getValue('memeToken');
        const memeToken = await MemeToken.at(memeTokenAddr);
        await memeToken.setAuthority(dsGuardAddr, opts);
    });

    setSmartContractAddress(smartContracts, ":meme-token-root", status.getValue('memeToken'));

    writeSmartContracts(smart_contracts_path, smartContracts, env);

    status.clean();
}
