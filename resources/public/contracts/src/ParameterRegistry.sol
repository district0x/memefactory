pragma solidity ^0.4.18;

import "Registry.sol";
import "ParameterChange.sol";

contract ParameterRegistry is Registry {

  function addRegistryEntry(address _registryEntry) {
    super.addRegistryEntry(_registryEntry);
    address lastEntry = db.getAddressValue(sha3("lastRegistryEntry"));
    if (lastEntry != 0x0) {
      db.setAddressValue(sha3("previousRegistryEntry", _registryEntry), lastEntry);
    }
    db.setAddressValue(sha3("lastRegistryEntry"), _registryEntry);
  }

  function applyParameterChange(address _parameterChange) {
    address prevParamChange = db.getAddressValue(sha3("previousRegistryEntry", _parameterChange));
    require(prevParamChange == 0x0 || ParameterChange(prevParamChange).wasApplied());
    db.transferOwnership(_parameterChange);
    ParameterChange(prevParamChange).applyChange(db);
  }
}

