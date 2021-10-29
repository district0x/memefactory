const fs = require('fs');
const path = require('path');
const edn = require("jsedn");

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

  smartContractsTemplate: (map, env) => {
    return `(ns memefactory.shared.smart-contracts-${env})
  (def smart-contracts
    ${map})
`;
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

  Status: class {
    constructor(id) {
      this.id = id;
      this.currentStep = 0;
      this.lastStep = -1;
      this.values = {};
      this._loadStatus();
    }

    async step(fn) {
      if (this.lastStep < this.currentStep) {
        console.log("Executing step: " + this.currentStep);
        let values = await fn(this);
        Object.assign(this.values, values);
        this.lastStep++;
        this._saveStatus();
      } else {
        console.log("Skipping previously executed step: " + this.currentStep);
      }
      this.currentStep++;
    }

    getValue(key) {
      return this.values[key];
    }

    _filename() {
      return path.resolve(__dirname, this.id + '_status.json');
    }

    _loadStatus() {
      if (fs.existsSync(this._filename())) {
        console.log("Previous execution detected. Loading status to resume")
        try {
          let data = fs.readFileSync(this._filename());
          let st = JSON.parse(data.toString());
          this.lastStep = st['lastStep'];
          this.values = st['values'];
        } catch (err) {
          console.warn("Failed to load status");
        }
      }
    }

    _saveStatus() {
      let data = JSON.stringify({'lastStep': this.lastStep, 'values': this.values});
      try {
        fs.writeFileSync(this._filename(), data)
      } catch (err) {
        console.warn("Cannot save state", err);
      }
    }

    clean() {
      try {
        fs.unlinkSync(this._filename());
      } catch (err) {
        console.warn("Failed to clean status", err);
      }
    }
  }

};

module.exports = utils;
