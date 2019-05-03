const {last, copy, linkBytecode, smartContractsTemplate} = require ("./utils.js");
const {contracts_build_directory, smart_contracts_path, parameters} = require ('../truffle.js');

const DankToken = artifacts.require("DankTokenCp");

/**
 * This migration transfers DANK to the last ganache account for development purposes
 *
 * Usage:
 * truffle migrate --network ganache/parity --reset --f 3 --to 3
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
    .then (() => {
      return DankToken.deployed ();
    })
    .then ((instance) => {
      return instance.transfer (last(accounts), 15e21, Object.assign(opts, {gas: 200000}));
    })
    .then (() => {
      return DankToken.deployed ();
    })
    .then ((instance) => {
      return [instance.balanceOf (address),
              instance.balanceOf (last(accounts))];
    })
    .then (promises =>  Promise.all (promises))
    .then ( (
      [balance1,
       balance2]) => {
         console.log ("@@@ DANK balance of:", address, balance1);
         console.log ("@@@ DANK balance of:", last(accounts), balance2);
       })
  
  deployer.then (function () {
    console.log ("Done");
  });

}
