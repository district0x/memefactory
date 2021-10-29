// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "./Registry.sol";
import "./ParamChange.sol";
import "./auth/DSGuard.sol";

/**
 * @title Central contract for TCR parameter change registry
 *
 * @dev Extends Registry contract with additional logic for applying parameter change into a registry EternalDb.
 */

contract ParamChangeRegistry is Registry {

  /**
   * @dev Gives ParamChange contract temporary permission to apply its parameter changes into EthernalDb
   * Only address of valid ParamChange contract can be passed
   * Permission must be taken back right after applying the change

   * @param _paramChange Address of ParamChange contract
   */

  function applyParamChange(ParamChange _paramChange) public {
    require(isRegistryEntry(address(_paramChange)), "ParamChangeRegistry: not a registry entry");
    DSGuard guard = DSGuard(address(_paramChange.db().authority()));
    guard.permit(address(_paramChange), address(_paramChange.db()), guard.ANY());
    _paramChange.applyChange();
    guard.forbid(address(_paramChange), address(_paramChange.db()), guard.ANY());
  }
}

