const { readSmartContractsFile, getSmartContractAddress, writeSmartContracts } = require ("./utils.js");
const { env, smart_contracts_path } = require ('../truffle.js');

const Migrations = artifacts.require("Migrations");
const DankFaucet = artifacts.require ("DankFaucetCp");

const smartContracts = readSmartContractsFile(smart_contracts_path);

const dankFaucetAddress = getSmartContractAddress(smartContracts, ":dank-faucet");
const migrationsAddress = getSmartContractAddress(smartContracts, ":migrations");

/**
 * This migration sets a gas price for provable (formerly oraclize) callback txs
 * yarn truffle migrate --network ganache --f 11 --to 11
 */
module.exports = function(deployer, network, accounts) {

  const address = accounts [0];
  const gas = 4e6;
  const opts = {gas: gas, from: address};

  const newGasPrice = 40e9 // 40 wei

  deployer.then (() => {
    console.log ("@@@ using Web3 version:", web3.version.api);
    console.log ("@@@ using address", address);
    console.log ("@@@ using network", network);
    console.log ("@@@ using DankFaucet", dankFaucetAddress);
    console.log ("@@@ using Migrations", migrationsAddress);

  })
    .then (() => DankFaucet.at (dankFaucetAddress))
    .then (dankFaucet => {
      dankFaucet.setCustomGasPrice (newGasPrice, Object.assign(opts, {gas: 1e6}));
    })
    .then (async () => {
      // set last ran tx
      const migrations = Migrations.at (migrationsAddress);
      await migrations.setCompleted (8, Object.assign(opts, {gas: 1e5, value: 0}));

      writeSmartContracts(smart_contracts_path, smartContracts, env);
      console.log ("Done");
    });

}
