const {last, copy, linkBytecode} = require ("./utils.js");
const fs = require('fs');
const {contracts_build_directory} = require ('../truffle.js');

copy ("DSGuard", "DSGuardCp", contracts_build_directory);
const DSGuard = artifacts.require("DSGuardCp");

copy ("MiniMeTokenFactory", "MiniMeTokenFactoryCp", contracts_build_directory);
const MiniMeTokenFactory = artifacts.require("MiniMeTokenFactoryCp");

copy ("DankToken", "DankTokenCp", contracts_build_directory);
const DankToken = artifacts.require("DankTokenCp");

copy ("DistrictConfig", "DistrictConfigCp", contracts_build_directory);
const DistrictConfig = artifacts.require("DistrictConfigCp");

copy ("District0xEmails", "District0xEmailsCp", contracts_build_directory);
const District0xEmails = artifacts.require ("District0xEmailsCp");

copy ("ParamChangeRegistry", "ParamChangeRegistryCp", contracts_build_directory);
const ParamChangeRegistry = artifacts.require("ParamChangeRegistryCp");

// copy artifacts for placeholder replacements
copy ("EternalDb", "MemeRegistryDb", contracts_build_directory);
const MemeRegistryDb = artifacts.require("MemeRegistryDb");

copy ("EternalDb", "ParamChangeRegistryDb", contracts_build_directory);
const ParamChangeRegistryDb = artifacts.require("ParamChangeRegistryDb");

copy ("Registry", "MemeRegistry", contracts_build_directory);
const MemeRegistry = artifacts.require("MemeRegistry");

copy ("MutableForwarder", "MemeRegistryForwarder", contracts_build_directory);
const MemeRegistryForwarder = artifacts.require("MemeRegistryForwarder");

copy ("MutableForwarder", "ParamChangeRegistryForwarder", contracts_build_directory);
const ParamChangeRegistryForwarder = artifacts.require("ParamChangeRegistryForwarder");

copy ("MemeToken", "MemeTokenCp", contracts_build_directory);
const MemeToken = artifacts.require("MemeTokenCp");

copy ("Meme", "MemeCp", contracts_build_directory);
const Meme = artifacts.require("MemeCp");

copy ("ParamChange", "ParamChangeCp", contracts_build_directory);
const ParamChange = artifacts.require("ParamChangeCp");

copy ("MemeFactory", "MemeFactoryCp", contracts_build_directory);
const MemeFactory = artifacts.require("MemeFactoryCp");

copy ("ParamChangeFactory", "ParamChangeFactoryCp", contracts_build_directory)
const ParamChangeFactory = artifacts.require("ParamChangeFactoryCp");

copy ("MutableForwarder", "MemeAuctionFactoryForwarder", contracts_build_directory);
const MemeAuctionFactoryForwarder = artifacts.require("MemeAuctionFactoryForwarder");

copy ("MemeAuctionFactory", "MemeAuctionFactoryCp", contracts_build_directory);
const MemeAuctionFactory = artifacts.require("MemeAuctionFactoryCp");

copy ("MemeAuction", "MemeAuctionCp", contracts_build_directory);
const MemeAuction = artifacts.require("MemeAuctionCp");

const registryPlaceholder = "feedfeedfeedfeedfeedfeedfeedfeedfeedfeed";
const dankTokenPlaceholder = "deaddeaddeaddeaddeaddeaddeaddeaddeaddead";
const forwarderTargetPlaceholder = "beefbeefbeefbeefbeefbeefbeefbeefbeefbeef";
const districtConfigPlaceholder = "abcdabcdabcdabcdabcdabcdabcdabcdabcdabcd";
const memeTokenPlaceholder = "dabbdabbdabbdabbdabbdabbdabbdabbdabbdabb";
const memeAuctionFactoryPlaceholder = "daffdaffdaffdaffdaffdaffdaffdaffdaffdaff";

module.exports = function(deployer, network, accounts) {

  const address = accounts [0];
  const gas = 4e6;
  const opts = {gas: gas, from: address};

  deployer.then (() => {
    console.log ("@@@ using Web3 version:", web3.version.api);
    console.log ("@@@ using address", address);
  });

  deployer.deploy (District0xEmails, Object.assign(opts, {gas: 500000}))
    .then (() => {
      return deployer.deploy (DSGuard, Object.assign(opts, {gas: gas}));
    })
    .then (instance => {
      // make deployed :ds-guard its own autority
      return instance.setAuthority(instance.address, Object.assign(opts, {gas: 100000}));
    })
    .then(() => {
      return deployer.deploy (MiniMeTokenFactory, Object.assign(opts, {gas: gas}));
    })
    .then (instance => {
      return deployer.deploy (DankToken, instance.address, 1e27, Object.assign(opts, {gas: gas}));
    })
    .then (instance => instance.totalSupply())
    .then (res => console.log ("@@@ DankToken/totalSupply:", res))
    .then (() => {
      return deployer.deploy (DistrictConfig, address, address, 0, Object.assign(opts, {gas: 1000000}));
    })
    .then (() => {
      return Promise.all([DSGuard.deployed(), DistrictConfig.deployed()]);
    })
    .then ((
      [dSGuard,
       districtConfig]) => {
         return districtConfig.setAuthority(dSGuard.address, Object.assign(opts, {gas: 100000}));
       })
    .then (() => {
      return deployer.deploy (MemeRegistryDb, Object.assign(opts, {gas: gas}));
    })
    .then (() => {
      return deployer.deploy (ParamChangeRegistryDb, Object.assign(opts, {gas: gas}));
    })
    .then (() => {
      return deployer.deploy (MemeRegistry, Object.assign(opts, {gas: gas}));
    })
    .then (instance => {
      linkBytecode(MemeRegistryForwarder, forwarderTargetPlaceholder, instance.address);
      return deployer.deploy(MemeRegistryForwarder, Object.assign(opts, {gas: gas}))
    })
    .then (instance => instance.target())
    .then (res => console.log ("@@@ MemeRegistryForwarder target:",  res))
    .then (() => {
      return deployer.deploy (ParamChangeRegistry, Object.assign(opts, {gas: gas}));
    })
    .then (instance => {
      linkBytecode(ParamChangeRegistryForwarder, forwarderTargetPlaceholder, instance.address);
      return deployer.deploy(ParamChangeRegistryForwarder, Object.assign(opts, {gas: gas}));
    })
    .then (() => {
      return Promise.all([MemeRegistryDb.deployed(), MemeRegistryForwarder.deployed()]);
    })
    .then ((
      [memeRegistryDb,
       memeRegistryForwarder]) => {
         var target = MemeRegistry.at (memeRegistryForwarder.address);
         return target.construct (memeRegistryDb.address, Object.assign(opts, {gas: 100000}));
       })
    .then (() => {
      return MemeRegistryForwarder.deployed ();
    })
    .then (instance => {
      var target = MemeRegistry.at (instance.address);
      return target.db ();
    })
    .then ( (res) => console.log ("@@@ MemeRegistry/db :", res))
    .then ( () => {
      return Promise.all ([ParamChangeRegistryDb.deployed (),
                           ParamChangeRegistryForwarder.deployed ()]);
    })
    .then ((
      [paramChangeRegistryDb,
       paramChangeRegistryForwarder]) => {
         var target = ParamChangeRegistry.at (paramChangeRegistryForwarder.address);
         return target.construct (paramChangeRegistryDb.address, Object.assign(opts, {gas: 100000}));
       })
    .then (() => {
      return Promise.all(
        [DSGuard.deployed (),
         ParamChangeRegistryForwarder.deployed ()]);
    })
    .then ((
      [dSGuard,
       paramChangeRegistryForwarder]) => {
         // Allow :param-change-registry-fwd to grand permissions to other contracts (for ParamChanges to apply changes)
         return dSGuard.permit(paramChangeRegistryForwarder.address, dSGuard.address, '0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff', Object.assign(opts, {gas: 100000}));
       })
    .then (() => {
      return MemeRegistryForwarder.deployed ();
    })
    .then ((instance) => {
      return deployer.deploy (MemeToken, instance.address, Object.assign(opts, {gas: 3200000}));
    })
    .then ((instance) => {
      return instance.registry();
    })
    .then ((res) => console.log ("@@@ MemeToken/registry", res))
    .then (() => {
      return Promise.all ([DankToken.deployed (),
                           MemeRegistryForwarder.deployed (),
                           DistrictConfig.deployed (),
                           MemeToken.deployed ()]);
    })
    .then ((
      [dankToken,
       memeRegistryForwarder,
       districtConfig,
       memeToken]) => {
         linkBytecode(Meme, dankTokenPlaceholder, dankToken.address);
         linkBytecode(Meme, registryPlaceholder, memeRegistryForwarder.address);
         linkBytecode(Meme, districtConfigPlaceholder, districtConfig.address);
         linkBytecode(Meme, memeTokenPlaceholder, memeToken.address);
         return deployer.deploy(Meme, Object.assign(opts, {gas: 6721975}));
       })
    .then (() => {
      return Promise.all ([DankToken.deployed (),
                           ParamChangeRegistryForwarder.deployed ()]);
    })
    .then ((
      [dankToken,
       paramChangeRegistryForwarder]) => {
         linkBytecode(ParamChange, dankTokenPlaceholder, dankToken.address);
         linkBytecode(ParamChange, registryPlaceholder, paramChangeRegistryForwarder.address);
         return deployer.deploy (ParamChange, Object.assign(opts, {gas: 6000000}));
       })
    .then (() => {
      return Promise.all ([Meme.deployed (),
                           MemeRegistryForwarder.deployed (),
                           DankToken.deployed (),
                           MemeToken.deployed ()]);
    })
    .then ((
      [meme,
       memeRegistryForwarder,
       dankToken,
       memeToken]) => {
         linkBytecode(MemeFactory, forwarderTargetPlaceholder, meme.address);
         return deployer.deploy (MemeFactory, memeRegistryForwarder.address, dankToken.address, memeToken.address,
                                 Object.assign(opts, {gas: gas}));
       })
    .then (() => {
      return Promise.all ([ParamChange.deployed (),
                           ParamChangeRegistry.deployed (),
                           DankToken.deployed ()]);
    })
    .then ((
      [paramChange,
       paramChangeRegistry,
       dankToken]) => {
         linkBytecode(ParamChangeFactory, forwarderTargetPlaceholder, paramChange.address);
         return deployer.deploy (ParamChangeFactory, paramChangeRegistry.address, dankToken.address,
                                 Object.assign(opts, {gas: gas}));
       })
    .then (() => {
      return MemeRegistryDb.deployed ();
    })
    .then ((instance) => {
      return instance.setUIntValues (['challengePeriodDuration',
                                      'commitPeriodDuration',
                                      'revealPeriodDuration',
                                      'deposit',
                                      'challengeDispensation',
                                      'voteQuorum',
                                      'maxTotalSupply',
                                      'maxAuctionDuration'].map (web3.sha3),
                                     [600,
                                      600,
                                      600,
                                      1e18,
                                      50,
                                      50,
                                      10,
                                      12096000],
                                     Object.assign(opts, {gas: 500000}));
    })
    .then (() => {
      return MemeRegistryDb.deployed ();
    })
    .then (instance => instance.getUIntValue (web3.sha3 ("deposit")))
    .then (res => console.log ("MemeRegistryDb/deposit:", res))
    .then (() => {
      return ParamChangeRegistryDb.deployed ();
    })
    .then ((instance) => {
      return instance.setUIntValues (['challengePeriodDuration',
                                      'commitPeriodDuration',
                                      'revealPeriodDuration',
                                      'deposit',
                                      'challengeDispensation',
                                      'voteQuorum'].map (web3.sha3),
                                     [600,
                                      600,
                                      600,
                                      1e18,
                                      50,
                                      50],
                                     Object.assign(opts, {gas: 500000}));
    })
    .then (() => {
      return Promise.all ([DSGuard.deployed (),
                           MemeRegistryDb.deployed ()]);
    })
    .then ((
      [dSGuard,
       memeRegistryDb]) => {
         //  make :ds-guard authority of both :meme-registry-db and :param-change-registry-db
         return memeRegistryDb.setAuthority(dSGuard.address, Object.assign(opts, {gas: 100000}));
       })
    .then (() => {
      return Promise.all ([DSGuard.deployed (),
                           ParamChangeRegistryDb.deployed ()]);
    })
    .then ((
      [dSGuard,
       paramChangeRegistryDb]) => {
         //  make :ds-guard authority of both :meme-registry-db and :param-change-registry-db
         return paramChangeRegistryDb.setAuthority(dSGuard.address, Object.assign(opts, {gas: 100000}));
       })
    .then (() => {
      return Promise.all ([MemeRegistryDb.deployed (),
                           ParamChangeRegistryDb.deployed ()]);
    })
    .then ((
      [memeRegistryDb,
       paramChangeRegistryDb]) => {
         // after authority is set, we can clean owner. Not really essential, but extra safety measure
         return Promise.all ([memeRegistryDb.setOwner(0, Object.assign(opts, {gas: 100000})),
                              paramChangeRegistryDb.setOwner (0, Object.assign(opts, {gas: 100000}))]);
       })
    .then (() => {
      return Promise.all([DSGuard.deployed (),
                          MemeRegistryForwarder.deployed (),
                          MemeRegistryDb.deployed ()]);
    })
    .then ((
      [dSGuard,
       memeRegistryForwarder,
       memeRegistryDb]) => {
         // allow :meme-registry-fwd to make changes into :meme-registry-db
         return dSGuard.permit(memeRegistryForwarder.address, memeRegistryDb.address, '0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff', Object.assign(opts, {gas: 100000}));
       })

    .then (() => {
      return Promise.all ([DSGuard.deployed (),
                           ParamChangeRegistryForwarder.deployed (),
                           MemeRegistryDb.deployed ()]);
    })

    .then ((
      [dSGuard,
       paramChangeRegistryForwarder,
       memeRegistryDb]) => {
         // allow :param-change-registry-fwd to make changes into :meme-registry-db (to apply ParamChanges)
         return dSGuard.permit(paramChangeRegistryForwarder.address, memeRegistryDb.address, '0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff', Object.assign(opts, {gas: 100000}));
       })
    .then (() => {
      return Promise.all ([DSGuard.deployed (),
                           ParamChangeRegistryForwarder.deployed (),
                           ParamChangeRegistryDb.deployed ()]);
    })
    .then ((
      [dSGuard,
       paramChangeRegistryForwarder,
       paramChangeRegistryDb]) => {
         // allow :param-change-registry-fwd to make changes into :param-change-registry-db
         return dSGuard.permit(paramChangeRegistryForwarder.address, paramChangeRegistryDb.address, '0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff', Object.assign(opts, {gas: 100000}));
       })
    .then (() => {
      return Promise.all ([MemeRegistryForwarder.deployed (),
                           MemeFactory.deployed ()]);
    })
    .then ((
      [memeRegistryForwarder,
       memeFactory]) => {
         var target = MemeRegistry.at (memeRegistryForwarder.address);
         return target.setFactory (memeFactory.address, true, Object.assign(opts, {gas: 100000}));
       })
    .then (() => {
      return Promise.all ([MemeRegistryForwarder.deployed (),
                           MemeFactory.deployed ()]);
    })
    .then ((
      [memeRegistryForwarder,
       memeFactory]) => {
         var target = MemeRegistry.at (memeRegistryForwarder.address);
         return target.isFactory (memeFactory.address);
       })
    .then (res => console.log ("@@@ MemeRegistry/isFactory", res))
    .then (() => {
      return Promise.all ([ParamChangeRegistryForwarder.deployed (),
                           ParamChangeFactory.deployed ()]);
    })
    .then (([paramChangeRegistryForwarder,
             paramChangeFactory]) => {
               var target = ParamChangeRegistry.at (paramChangeRegistryForwarder.address);
               return target.setFactory (paramChangeFactory.address, true, Object.assign(opts, {gas: 100000}));
             })
    .then (() => {
      return deployer.deploy (MemeAuctionFactoryForwarder, Object.assign(opts, {gas: gas}));
    }).then (() => {
      return Promise.all ([DSGuard.deployed (),
                           MemeAuctionFactoryForwarder.deployed ()]);
    })
    .then ((
      [dSGuard,
       memeAuctionFactoryForwarder]) => {
         return memeAuctionFactoryForwarder.setAuthority(dSGuard.address, Object.assign(opts, {gas: 100000}));
       })
    .then (() => {
      return Promise.all ([MemeAuctionFactoryForwarder.deployed (),
                           MemeRegistryForwarder.deployed (),
                           DistrictConfig.deployed (),
                           MemeToken.deployed ()]);
    })
    .then ((
      [memeAuctionFactoryForwarder,
       memeRegistryForwarder,
       districtConfig,
       memeToken]
    ) => {
      linkBytecode(MemeAuction, memeAuctionFactoryPlaceholder, memeAuctionFactoryForwarder.address);
      linkBytecode(MemeAuction, registryPlaceholder, memeRegistryForwarder.address);
      linkBytecode(MemeAuction, districtConfigPlaceholder, districtConfig.address);
      linkBytecode(MemeAuction, memeTokenPlaceholder, memeToken.address);
      return deployer.deploy(MemeAuction, Object.assign(opts, {gas: 4000000}));
    })
    .then ((instance) => {
      linkBytecode(MemeAuctionFactory, forwarderTargetPlaceholder, instance.address);
      return Promise.all ([deployer.deploy(MemeAuctionFactory, Object.assign(opts, {gas: 2000000})),
                           MemeAuctionFactoryForwarder.deployed ()]);
    }).then ((
      [memeAuctionFactory,
       memeAuctionFactoryForwarder]) => {
         return memeAuctionFactoryForwarder.setTarget(memeAuctionFactory.address, Object.assign(opts, {gas: 100000}));
       })
    .then (() => {
      return Promise.all([MemeToken.deployed (),
                          MemeAuctionFactoryForwarder.deployed ()]);
    })
    .then ((
      [memeToken,
       memeAuctionFactoryForwarder]) => {
         var target = MemeAuctionFactory.at (memeAuctionFactoryForwarder.address);
         return target.construct (memeToken.address, Object.assign(opts, {gas: 200000}));
       })
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
    .then (() => {
      return Promise.all ([DankToken.deployed (),
                           MemeFactory.deployed ()])
    })
    .then ((
      [dankToken,
       memeFactory]) => {
         var payload = memeFactory.contract.createMeme.getData(last(accounts), "QmWip6bd1hZXqXMiwzgNkS8dvYMh7ZD9VcjcLSooyEqx1F", 1);
         return dankToken.approveAndCall (memeFactory.address,
                                          1e18,
                                          payload,
                                          {from: last(accounts), gas: 4000000});
       })
    .then (tx => console.log ("@@@ DankToken/approveAndCall tx:", tx.receipt.transactionHash, "successful"));

  // TODO: json -> edn
  deployer.then (function () {
    return [
      DSGuard.deployed (),
      MiniMeTokenFactory.deployed (),
      DankToken.deployed (),
      DistrictConfig.deployed (),
      MemeRegistryDb.deployed (),
      ParamChangeRegistryDb.deployed (),
      MemeRegistry.deployed (),
      ParamChangeRegistry.deployed (),
      MemeRegistryForwarder.deployed (),
      ParamChangeRegistryForwarder.deployed (),
      MemeToken.deployed (),
      Meme.deployed (),
      ParamChange.deployed (),
      MemeFactory.deployed (),
      ParamChangeFactory.deployed (),
      MemeAuctionFactoryForwarder.deployed (),
      MemeAuctionFactory.deployed (),
      MemeAuction.deployed (),
      District0xEmails.deployed ()];
  }).then ( (promises) => {
    return Promise.all(promises);
  }).then ((
    [dSGuard,
     miniMeTokenFactory,
     dankToken,
     districtConfig,
     memeRegistryDb,
     paramChangeRegistryDb,
     memeRegistry,
     paramChangeRegistry,
     memeRegistryForwarder,
     paramChangeRegistryForwarder,
     memeToken,
     meme,
     paramChange,
     memeFactory,
     paramChangeFactory,
     memeAuctionFactoryForwarder,
     memeAuctionFactory,
     memeAuction,
     district0xEmails]) => {
       var smartContracts = JSON.stringify ({
         "district-config" : {"name" : "DistrictConfig",
                              "address" : districtConfig.address},
         "ds-guard" : {"name" : "DSGuard",
                       "address": dSGuard.address},
         "param-change-registry" : {"name" : "ParamChangeRegistry",
                                    "address": paramChangeRegistry.address},
         "param-change-registry-db" : {"name" : "EternalDb",
                                       "address": paramChangeRegistryDb.address},
         "meme-registry-db" : {"name" : "EternalDb",
                               "address" : memeRegistryDb.address},
         "param-change" : {"name" : "ParamChange",
                           "address" : paramChange.address},
         "minime-token-factory" : {"name" : "MiniMeTokenFactory",
                                   "address" : miniMeTokenFactory.address},
         "meme-auction-factory" : {"name" : "MemeAuctionFactory",
                                   "address" : memeAuctionFactory.address},
         "meme-auction" : {"name" : "MemeAuction",
                           "address" : memeAuction.address},
         "param-change-factory" : {"name" : "ParamChangeFactory",
                                   "address": paramChangeFactory.address},
         "param-change-registry-fwd" : {"name" : "MutableForwarder",
                                        "address" : paramChangeRegistryForwarder.address},
         "meme-factory" : {"name": "MemeFactory",
                           "address" : MemeFactory.address},
         "meme-token" : {"name" : "MemeToken",
                         "address" : memeToken.address},
         "DANK" : {"name" : "DankToken",
                   "address" : dankToken.address},
         "meme-registry" : {"name" : "Registry",
                            "address" : memeRegistry.address},
         "meme" : {"name" : "Meme",
                   "address": meme.address},
         "meme-registry-fwd" : {"name" : "MutableForwarder",
                                "address" : memeRegistryForwarder.address},
         "meme-auction-factory-fwd" : {"name" : "MutableForwarder",
                                       "address" : memeAuctionFactoryForwarder.address},
         "district0x-emails" : {"name" : "District0xEmails",
                                "address" : district0xEmails.address}

       });
       console.log (smartContracts);
       fs.writeFile('smartContracts.json', smartContracts);
     });

  deployer.then (function () {
    console.log ("Done");
  });

}
