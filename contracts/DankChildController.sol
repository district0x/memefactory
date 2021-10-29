// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "./token/minime/TokenController.sol";
import "./token/minime/MiniMeToken.sol";
import "./auth/DSAuth.sol";

/**
 * @title Controller for allowing minting/burning Dank tokens based on white lists
 *
 */

contract DankChildController is TokenController, DSAuth {
    MiniMeToken public miniMeToken;

    constructor (address payable _miniMeToken) {
        owner = msg.sender;
        miniMeToken = MiniMeToken(_miniMeToken);
    }

    function proxyPayment(address) public payable override returns(bool) {
        return true;
    }

    function onTransfer(address, address, uint) public override pure returns(bool) {
        return true;
    }

    function onApprove(address, address, uint) public override pure returns(bool) {
        return true;
    }

    function mint(address _owner, uint _amount) public auth {
        miniMeToken.generateTokens(_owner, _amount);
    }

    function burn(address _owner, uint _amount) public auth {
        miniMeToken.destroyTokens(_owner, _amount);
    }

}