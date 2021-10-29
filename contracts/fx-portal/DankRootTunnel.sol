// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import { FxBaseRootTunnel } from "./tunnel/FxBaseRootTunnel.sol";
import { MiniMeToken, ApproveAndCallFallBack} from "../token/minime/MiniMeToken.sol";

/**
 * @title DankRootTunnel
 */
contract DankRootTunnel is FxBaseRootTunnel, ApproveAndCallFallBack {
    event FxDepositERC20(address indexed depositor, address indexed receiver, uint256 amount);
    event FxWithdrawERC20(address indexed receiver, uint256 amount);

    MiniMeToken public rootToken;

    constructor(address _checkpointManager, address _fxRoot, address _rootToken) FxBaseRootTunnel(_checkpointManager, _fxRoot) {
        rootToken = MiniMeToken(payable(_rootToken));
    }

    function deposit(address receiver, uint256 amount) public {
        depositFrom(msg.sender, receiver, amount);
    }

    function depositFrom(address depositor, address receiver, uint256 amount) public {
        // transfer from depositor to this contract
        // cannot use safeTransferFrom because original DankToken uses an old (incompatible) ERC721 contract version
        rootToken.transferFrom(
            depositor,
            address(this), // manager contract
            amount
        );

        bytes memory message = abi.encode(receiver, amount);
        _sendMessageToChild(message);
        emit FxDepositERC20(depositor, receiver, amount);
    }

    // exit processor
    function _processMessageFromChild(bytes memory data) internal override {
        (address to, uint256 amount) = abi.decode(data, (address, uint256));
        // transfer locked tokens to owner
        rootToken.transfer(to, amount);
        emit FxWithdrawERC20(to, amount);
    }

   /**
   * @dev Function called by MiniMeToken when somebody calls approveAndCall on it.
   * This way token can be transferred to a recipient in a single transaction together with execution
   * of additional logic

   * @param _from Sender of transaction approval
   * @param _amount Amount of approved tokens to transfer
   * @param _token Token that received the approval
   * @param _data Bytecode of a function and passed parameters, that should be called after token approval
   */
    function receiveApproval(
        address _from,
        uint256 _amount,
        address _token,
        bytes memory _data)
    public override
    {
        (bool success,) = address(this).call(_data);
        require(success);
    }
}
