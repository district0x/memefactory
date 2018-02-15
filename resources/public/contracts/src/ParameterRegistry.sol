pragma solidity ^0.4.18;

import "Registry.sol";
import "ParameterChange.sol";

contract ParameterRegistry is Registry {

  function ParameterRegistry(address _eternalStorage)
  Registry(_eternalStorage)
  {}

  function addRegistryEntry(address _registryEntry) {
    super.addRegistryEntry(_registryEntry);
    address lastEntry = eternalStorage.getAddressValue(sha3("lastRegistryEntry"));
    if (lastEntry != 0x0) {
      eternalStorage.setAddressValue(sha3("previousRegistryEntry", _registryEntry), lastEntry);
    }
    eternalStorage.setAddressValue(sha3("lastRegistryEntry"), _registryEntry);
  }

  function applyParameterChange(address _parameterChange) {
    address prevParamChange = eternalStorage.getAddressValue(sha3("previousRegistryEntry", _parameterChange));
    require(prevParamChange == 0x0 || ParameterChange(prevParamChange).wasApplied());
    eternalStorage.transferOwnership(_parameterChange);
    ParameterChange(prevParamChange).applyChange(eternalStorage);
  }
}

