// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "./token/minime/MiniMeToken.sol";

/**
 * @title Token used for curation of MemeFactory TCR
 *
 * @dev Standard MiniMe Token aimed to port Dank tokens to children (L2) networks.
 * This contract does not mint any token during creation, but allows a controller to do so.
 * The idea is to mint new ones by the child bridge after they are locked in the root bridge,
 * and burn when withdrawing them back
 */

contract DankTokenChild is MiniMeToken {
    constructor (address _tokenFactory)
    MiniMeToken(
        _tokenFactory,
        payable(address(0)),
        0,
        "Dank Token",
        18,
        "DANK",
        true
    )
    {

    }
}