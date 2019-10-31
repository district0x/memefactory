const {last, copy, linkBytecode, smartContractsTemplate, readSmartContractsFile, getSmartContractAddress} = require ("./utils.js");
const {contracts_build_directory, smart_contracts_path, parameters} = require ('../truffle.js');

const DankToken = artifacts.require("DankTokenCp");

const smartContracts = readSmartContractsFile(smart_contracts_path);
const dankTokenAddr = getSmartContractAddress(smartContracts, ":DANK");

/**
 * This migration transfers DANK to ganache accounts for development purposes
 *
 * truffle migrate --network ganache --f 3 --to 3
 */
module.exports = function(deployer, network, accounts) {

  const address = accounts [0];
  const gas = 4e6;
  const opts = {gas: gas, from: address};

  deployer.then (() => {
    console.log ("@@@ using Web3 version:", web3.version.api);
    console.log ("@@@ using address", address);
  });

  deployer
    .then (() => DankToken.at (dankTokenAddr))
    .then ((instance) => {
      let txs = [];
      for (i = 0; i < accounts.length; i++) {
        txs.push (instance.transfer (accounts [i], 1000e18, Object.assign(opts, {gas: 200000})));
      }
      return txs;
    })
    .then (() => DankToken.at (dankTokenAddr))
    .then ((instance) => {
      let txs = [];
      for (i = 0; i < accounts.length; i++) {
        txs.push (instance.balanceOf (accounts [i]));
      }
      return txs;
    })
    .then (promises => Promise.all (promises))
    .then ((balances) => {
      for (i = 0; i < balances.length; i++) {
        console.log ("@@@ DANK balance of:", accounts [i], balances [i]);
      }
    })
    .then ( () => console.log ("Done"));

}
