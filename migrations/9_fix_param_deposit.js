const {readSmartContractsFile,getSmartContractAddress, setSmartContractAddress, writeSmartContracts, linkBytecode} = require ("./utils.js");

const {parameters, smart_contracts_path, env} = require ('../truffle.js');
const web3Utils = require('web3-utils');

var smartContracts = readSmartContractsFile(smart_contracts_path);
var paramChangeRegistryDbAddr = getSmartContractAddress(smartContracts, ":param-change-registry-db");
var dSGuardAddr = getSmartContractAddress(smartContracts, ":ds-guard");

const EternalDb = artifacts.require("EternalDb");
const DSGuard = artifacts.require("DSGuard");
const newDepositValue = 250000e18;

/**
 * This migration fixes Parameter Deposit that was initialy set to 1000000000 DANK
 * @madvas : we’ve put it there before so nobody can propose param change, we need to change that value direct way through EternalDb
 *
 * Usage:
 * truffle migrate --network ganache/parity --reset --f 9 --to 9
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
    .then (async () => {
      var paramChangeRegistryDbInstance = EternalDb.at(paramChangeRegistryDbAddr);
      var dSGurardInstance = DSGuard.at(dSGuardAddr);
      console.log("Changing eternal db at address", paramChangeRegistryDbAddr);
      console.log("Setting new param changes deposit value to ", newDepositValue);

      await dSGurardInstance.permit(address, paramChangeRegistryDbAddr, '0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff', Object.assign(opts, {gas: 100000}));

      await paramChangeRegistryDbInstance.setUIntValue (web3Utils.soliditySha3('deposit'),
                                                        newDepositValue,
                                                        Object.assign(opts, {gas: 500000}));

      await dSGurardInstance.forbid(address, paramChangeRegistryDbAddr, '0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff', Object.assign(opts, {gas: 100000}));
})

  deployer.then (function () {
    console.log ("Done");
  });

}
