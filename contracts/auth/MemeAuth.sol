// SPDX-License-Identifier: MIT
import "./DSAuth.sol";
import "./DSGuard.sol";
import "../Registry.sol";


/**
 * DSAuthority to allow either registry entry (Meme) or ACL-based auth (DSGuard)
 */
contract MemeAuth is DSAuthority {
    Registry public registry;
    DSGuard public dsGuard;

    constructor(Registry _registry, DSGuard _dsGuard) {
        registry = _registry;
        dsGuard = _dsGuard;
    }

    function canCall(address src, address dst, bytes4 sig) public view override returns (bool) {
        return (address(registry) != address(0) && registry.isRegistryEntry(src))
            || (address(dsGuard) != address(0) && dsGuard.canCall(src, dst, sig));
    }
}
