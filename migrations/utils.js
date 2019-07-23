const fs = require('fs');
const edn = require("jsedn");
const test = {
  f1 : () => {return "Hello";},
  f2 : () => {console.log(this.f1() + " world")}
}

function smartContractsTemplate (map, env) {
  return `(ns memefactory.shared.smart-contracts-${env})

  (def smart-contracts
    ${map})
`;
}

function encodeSmartContracts (smartContracts) {
  if (Array.isArray(smartContracts)) {
    smartContracts = new edn.Map(smartContracts);
  }
  var contracts = edn.encode(smartContracts);
  console.log(contracts);
  return contracts;
};

const utils = {

  last: (array) => {
    return array[array.length - 1];
  },

  copy: (srcName, dstName, contracts_build_directory, network, address) => {

    let buildPath = contracts_build_directory;

    const srcPath = buildPath + srcName + '.json';
    const dstPath = buildPath + dstName + '.json';

    const data = require(srcPath);
    data.contractName = dstName;

    // Save address when given
    if (network && address) {
      data.networks = {};

      // Copy existing networks
      if (fs.existsSync(dstPath)) {
        const existing = require(dstPath);
        data.networks = existing.networks;
      }

      data.networks[network.toString()] = {
        address: address
      };
    }
    fs.writeFileSync(dstPath, JSON.stringify(data, null, 2), { flag: 'w' });
  },

  linkBytecode: (contract, placeholder, replacement) => {
    var placeholder = placeholder.replace('0x', '');
    var replacement = replacement.replace('0x', '');
    var bytecode = contract.bytecode.split(placeholder).join(replacement);
    contract.bytecode = bytecode;
  },

  readSmartContractsFile: (smartContractsPath) => {
    var content = fs.readFileSync(smartContractsPath, "utf8");

    content = content.replace(/\(ns.*\)/gm, "");
    content = content.replace(/\(def smart-contracts/gm, "");
    content = content.replace(/\)$/gm, "");

    return edn.parse(content);
  },

  setSmartContractAddress: (smartContracts, contractKey, newAddress) => {
  var contract = edn.atPath(smartContracts, contractKey);
  contract = contract.set(edn.kw(":address"), newAddress);
  return smartContracts.set(edn.kw(contractKey), contract);
  },

  getSmartContractAddress: (smartContracts, contractKey) => {
    try {
      return edn.atPath(smartContracts, contractKey + " :address");
    } catch (e) {
      return null;
    }
  },

  writeSmartContracts: (smartContractsPath, smartContracts, env) => {
    console.log("Writing to smart contract file: " + smartContractsPath);
    fs.writeFileSync(smartContractsPath, smartContractsTemplate(encodeSmartContracts(smartContracts), env));
  },

  writeSmartContractsFile: function (env,
                                     smart_contracts_path,
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
                                      district0xEmails,
                                      dankFaucet]) {

                               var smartContracts = edn.encode(
                                 new edn.Map([

                                   edn.kw(":district-config"), new edn.Map([edn.kw(":name"), "DistrictConfig",
                                                                            edn.kw(":address"), districtConfig.address]),

                                   edn.kw(":ds-guard"), new edn.Map([edn.kw(":name"), "DSGuard",
                                                                     edn.kw(":address"), dSGuard.address]),

                                   edn.kw(":param-change-registry"), new edn.Map([edn.kw(":name"), "ParamChangeRegistry",
                                                                                  edn.kw(":address"), paramChangeRegistry.address]),

                                   edn.kw(":param-change-registry-db"), new edn.Map([edn.kw(":name"), "EternalDb",
                                                                                     edn.kw(":address"), paramChangeRegistryDb.address]),

                                   edn.kw(":meme-registry-db"), new edn.Map([edn.kw(":name"), "EternalDb",
                                                                             edn.kw(":address"), memeRegistryDb.address]),

                                   edn.kw(":param-change"), new edn.Map([edn.kw(":name"), "ParamChange",
                                                                         edn.kw(":address"), paramChange.address]),

                                   edn.kw(":minime-token-factory"), new edn.Map([edn.kw(":name"), "MiniMeTokenFactory",
                                                                                 edn.kw(":address"), miniMeTokenFactory.address]),

                                   edn.kw(":meme-auction-factory"), new edn.Map([edn.kw(":name"), "MemeAuctionFactory",
                                                                                 edn.kw(":address"), memeAuctionFactory.address]),

                                   edn.kw(":meme-auction"), new edn.Map([edn.kw(":name"), "MemeAuction",
                                                                         edn.kw(":address"), memeAuction.address]),

                                   edn.kw(":param-change-factory"), new edn.Map([edn.kw(":name"), "ParamChangeFactory",
                                                                                 edn.kw(":address"), paramChangeFactory.address]),

                                   edn.kw(":param-change-registry-fwd"), new edn.Map([edn.kw(":name"), "MutableForwarder",
                                                                                      edn.kw(":address"), paramChangeRegistryForwarder.address,
                                                                                      edn.kw(":forwards-to"), edn.kw(":param-change-registry")]),

                                   edn.kw(":meme-factory"), new edn.Map([edn.kw(":name"), "MemeFactory",
                                                                         edn.kw(":address"), memeFactory.address]),

                                   edn.kw(":meme-token"), new edn.Map([edn.kw(":name"), "MemeToken",
                                                                       edn.kw(":address"), memeToken.address]),

                                   edn.kw(":DANK"), new edn.Map([edn.kw(":name"), "DankToken",
                                                                 edn.kw(":address"), dankToken.address]),

                                   edn.kw(":meme-registry"), new edn.Map([edn.kw(":name"), "Registry",
                                                                          edn.kw(":address"), memeRegistry.address]),

                                   edn.kw(":meme"), new edn.Map([edn.kw(":name"), "Meme",
                                                                 edn.kw(":address"), meme.address]),

                                   edn.kw(":meme-registry-fwd"), new edn.Map([edn.kw(":name"), "MutableForwarder",
                                                                              edn.kw(":address"), memeRegistryForwarder.address,
                                                                              edn.kw(":forwards-to"), edn.kw(":meme-registry")]),

                                   edn.kw(":meme-auction-factory-fwd"), new edn.Map([edn.kw(":name"), "MutableForwarder",
                                                                                     edn.kw(":address"), memeAuctionFactoryForwarder.address,
                                                                                     edn.kw(":forwards-to"), edn.kw(":meme-auction-factory")]),

                                   edn.kw(":district0x-emails"), new edn.Map([edn.kw(":name"), "District0xEmails",
                                                                              edn.kw(":address"), district0xEmails.address]),

                                   edn.kw(":dank-faucet"), new edn.Map([edn.kw(":name"), "DankFaucet",
                                                                        edn.kw(":address"), dankFaucet.address]),

                                 ]));

                               console.log (smartContracts);
                               fs.writeFileSync(smart_contracts_path, smartContractsTemplate(smartContracts, env));
                             }

};

module.exports = utils;
