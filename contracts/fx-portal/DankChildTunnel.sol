// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import { FxBaseChildTunnel } from './tunnel/FxBaseChildTunnel.sol';
import { DankChildController } from '../DankChildController.sol';

/** 
 * @title DankChildTunnel
 */
contract DankChildTunnel is FxBaseChildTunnel {

    DankChildController public childToken;

    constructor(address _fxChild, address _childToken) FxBaseChildTunnel(_fxChild) {
        childToken = DankChildController(_childToken);
    }

    function withdraw(uint256 amount) public {
        // withdraw tokens
        childToken.burn(msg.sender, amount);

        // send message to root regarding token burn so it allows tokens transfer
        _sendMessageToRoot(abi.encode(msg.sender, amount));
    }

    function _processMessageFromRoot(uint256 /* stateId */, address sender, bytes memory data) internal override validateSender(sender) {
        (address to, uint256 amount) = abi.decode(data, (address, uint256));
        // mint the same amount of tokens which have been locked in root
        childToken.mint(to, amount);
    }
}
