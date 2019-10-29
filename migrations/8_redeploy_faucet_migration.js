const BigNumber = require('bignumber.js');
const { linkBytecode, copy } = require ("./utils.js");
const { parameters, contracts_build_directory } = require ('../truffle.js');

const DSGuard = artifacts.require("DSGuard");
const DankToken = artifacts.require("DankToken");
copy ("DankFaucet", "DankFaucetCp", contracts_build_directory);
const DankFaucet = artifacts.require ("DankFaucetCp");

const dankTokenPlaceholder = "deaddeaddeaddeaddeaddeaddeaddeaddeaddead";

// TODO : adjust addresses these

// ganache
const dankFaucetAddress = "";
const dankTokenAddress = "";
const dSGuardAddress = "";

// ropsten
// const dankFaucetAddress = "0x8993009f44cd657cf869e9ac30c189206e3b6cef";
// const dankTokenAddress = "0xeda9bf9199fab6790f43ee21cdce048781f58302";
// const dSGuardAddress = "0xab4d684b2cc21ea99ee560a0f0d1490b09b09127";

// mainnet
// const dankFaucetAddress = "0x7abdcd059a60ad6d240a62be3fe0293fb2b65c19";
// const dankTokenAddress = "0x0cb8d0b37c7487b11d57f1f33defa2b1d3cfccfe";
// const dSGuardAddress = "0x5d0457f58ed4c115610a2253070a11fb82065403";

/**
 * This migration drains old faucet and redeploys it seeding new Faucet with ETH and DANK
 *
 * Usage:
 * truffle migrate --reset --f 8 --to 8 --network ganache
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
    .then ((dankFaucet) => console.log ("@@@ new DankFaucet address:", dankFaucet.address))
  ;
}
