const { readSmartContractsFile, getSmartContractAddress } = require ("./utils.js");
const { parameters, smart_contracts_path } = require ('../truffle.js');

const Migrations = artifacts.require("Migrations");

var smartContracts = readSmartContractsFile(smart_contracts_path);
const migrationsAddress = getSmartContractAddress(smartContracts, ":migrations");

/**
 * truffle exec ./migrations/check_ran_migrations.js --network ganache
 */
module.exports = function(callback) {

  const address = web3.eth.accounts[0] ;
  const gas = 4e6;
  const opts = {gas: gas, from: address};

  web3.version.getNetwork( (error, network) => {

        console.log ("@@@ network:", network);

  });

  Migrations.at (migrationsAddress).last_completed_migration ()
    .then ((number) => {

// TODO
   // count migrations
      // exit if you have migrations which have not been run of this network
      process.exit(1);

      console.log ("@@@ last completed migration:", number)
    })

}
