// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "./DankToken.sol";
import "./auth/DSAuth.sol";

/**
 * A Faucet for getting initial DANK tokens. Provide a hashed account id (e.g., twitter account), and an address
 * receive a one-time allotment of DANK.
 **/
contract DankFaucet is DSAuth {
    DankToken public constant dankToken = DankToken(payable(0xdeaDDeADDEaDdeaDdEAddEADDEAdDeadDEADDEaD));

    uint public allotment;

    mapping(bytes32 => uint) public allocatedDank;

    // An event for communicating successful transfer of funds
    event DankTransferEvent(bytes32 hashedAccountId, uint allotment);

    // An event fired when a person's allotment is reset by the faucet.
    event DankResetEvent(bytes32 hashedAccountId);

    // An event fired to indicate the allotment has been (re)set
    event ResetAllotmentEvent(uint allotment);

    constructor (uint _allotment) {
        allotment = _allotment;
        emit ResetAllotmentEvent(allotment);
    }

    /**
     * Send dank to a user who has not requested any allotment yet
     */
    function sendDank(bytes32 hashedAccountId, address to) public auth {

        uint dankBalance = dankToken.balanceOf(address(this));
        require(dankBalance > allotment, "Faucet has run out of DANK");

        uint previouslyAllocatedDank = allocatedDank[hashedAccountId];
        require(previouslyAllocatedDank <= 0, "DANK already allocated");

        require(dankToken.transfer(to, allotment), "DANK transfer failed");
        allocatedDank[hashedAccountId] = allotment;

        emit DankTransferEvent(hashedAccountId, allotment);
    }

    /**
     * Allow the owner to reset the limits on the amount distributed by the faucet.
     */
    function resetAllotment(uint _allotment) public auth {
        allotment = _allotment;
        emit ResetAllotmentEvent(allotment);
    }

    /**
     * Allow the owner to reset the DANK we've allocated to an account.
     */
    function resetAllocatedDank(bytes32 hashedAccountId) public auth {
        delete allocatedDank[hashedAccountId];
        emit DankResetEvent(hashedAccountId);
    }

    /**
     * Allow the owner to withdraw DANK in this contract
     */
    function withdrawDank() public auth {
        dankToken.transfer(msg.sender, dankToken.balanceOf(address(this)));
    }

}