// const BigNumber = require('bignumber.js');
const { linkBytecode, copy, readSmartContractsFile, getSmartContractAddress, setSmartContractAddress, writeSmartContracts } = require ("./utils.js");
const { parameters, contracts_build_directory, smart_contracts_path, env } = require ('../truffle.js');

const DSGuard = artifacts.require("DSGuard");
const DankToken = artifacts.require("DankToken");
copy ("DankFaucet", "DankFaucetCp", contracts_build_directory);
const DankFaucet = artifacts.require ("DankFaucetCp");

const dankTokenPlaceholder = "deaddeaddeaddeaddeaddeaddeaddeaddeaddead";

const smartContracts = readSmartContractsFile(smart_contracts_path);
const dankTokenAddress = getSmartContractAddress(smartContracts, ":DANK");
const dankFaucetAddress = getSmartContractAddress(smartContracts, ":dank-faucet");
const dSGuardAddress = getSmartContractAddress(smartContracts, ":ds-guard");

/**
 * This migration drains old faucet and redeploys it seeding new Faucet with ETH and DANK
 * truffle migrate --network infura-ropsten --f 8 --to 8
 */
module.exports = function(deployer, network, accounts) {

  const address = accounts [0];
  const gas = 4e6;
  const opts = {gas: gas, from: address};

  let balances = {};

  deployer.then (() => {
    console.log ("@@@ using Web3 version:", web3.version.api);
    console.log ("@@@ using address", address);
  })

  // get eth and dank balance
    .then (() => Promise.all ([
      DankFaucet.at (dankFaucetAddress),
      DankToken.at (dankTokenAddress)
    ]))
    .then (([
      dankFaucet,
      dankToken
    ]) => Promise.all ([
      dankFaucet.getBalance(),
      dankToken.balanceOf (dankFaucetAddress)
      // new BigNumber(0.2e18),
      // new BigNumber(4865000e18)
    ]))
    .then (([
      ethBalance,
      dankBalance
    ]) => {
      Object.assign (balances, {eth: ethBalance, dank: dankBalance})
      console.log ("@@@ Faucet ETH balance: ", balances.eth);
      console.log ("@@@ Faucet DANK balance: ", balances.dank);
    })

  // drain eth and dank
    .then (() => DankFaucet.at (dankFaucetAddress))
    .then ((dankFaucet) => Promise.all ([
      dankFaucet.withdrawEth (Object.assign(opts, {gas: 0.5e6})),
      dankFaucet.withdrawDank (Object.assign(opts, {gas: 0.5e6}))
    ]))
    .then (([
      tx1,
      tx2
    ]) => console.log ("@@@ Faucet succesfully drained of all funds"))

  // redeploy
    .then (() => {
      linkBytecode(DankFaucet, dankTokenPlaceholder, dankTokenAddress);
      return deployer.deploy(DankFaucet, parameters.dankFaucet.allotment, Object.assign(opts, {gas: 4e6}));
    })

    .then (() => Promise.all ([DSGuard.at(dSGuardAddress),
                               DankFaucet.deployed()]))
    .then (([dSGuard,
             dankFaucet]) => dankFaucet.setAuthority(dSGuard.address, Object.assign(opts, {gas: 0.5e6, value: 0})))

  // seed with the same eth and dank amount
    .then (() => Promise.all ([DankToken.at (dankTokenAddress),
                               DankFaucet.at (dankFaucetAddress),
                               DankFaucet.deployed()]))
    .then (([dankToken,
             dankFaucet,
             newDankFaucet]) =>
           Promise.all ([
             dankToken.transfer (newDankFaucet.address, balances.dank, Object.assign(opts, {gas: 0.5e6})),
             newDankFaucet.sendEth (Object.assign(opts, {gas: 0.5e6, value: balances.eth}))]))
    .then (() => DankFaucet.deployed())
    .then ((dankFaucet) => {

      console.log ("@@@ new DankFaucet address:", dankFaucet.address)
      setSmartContractAddress(smartContracts, ":dank-faucet", dankFaucet.address);
      writeSmartContracts(smart_contracts_path, smartContracts, env);

      console.log ("Done");

    });
}
