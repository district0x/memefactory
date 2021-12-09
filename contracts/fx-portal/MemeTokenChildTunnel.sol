// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import { FxBaseChildTunnelInitializable } from './tunnel/FxBaseChildTunnelInitializable.sol';
import { MemeTokenChild } from '../MemeTokenChild.sol';
import "@openzeppelin/contracts-upgradeable/proxy/utils/UUPSUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/access/OwnableUpgradeable.sol";
import "@openzeppelin/contracts/proxy/ERC1967/ERC1967Proxy.sol";  // only for compiler

/** 
 * @title MemeTokenChildTunnel
 */
contract MemeTokenChildTunnel is FxBaseChildTunnelInitializable, UUPSUpgradeable, OwnableUpgradeable {
    MemeTokenChild public childToken;

    uint256 public constant BATCH_LIMIT = 20;
    bytes4 public constant SINGLE = bytes4(keccak256("SINGLE"));
    bytes4 public constant BATCH = bytes4(keccak256("BATCH"));

    function initialize(address _fxChild, address _childToken) initializer public {
        __Ownable_init();
        __FxBaseChildTunnel_init(_fxChild);
        childToken = MemeTokenChild(_childToken);
    }

    function _authorizeUpgrade(address newImplementation) internal override onlyOwner
    {}

    function withdraw(uint256 tokenId) external {
        // send message to root regarding token burn so it allows tokens transfer
        _sendMessageToRoot(abi.encode(msg.sender, SINGLE, abi.encode(tokenId, childToken.encodeTokenMetadata(tokenId))));

        // check owner of the NFT
        require(msg.sender == childToken.ownerOf(tokenId), "MemeTokenChildTunnel: INVALID_OWNER");

        // withdraw tokens
        childToken.burn(tokenId);
    }

    function withdrawBatch(uint256[] calldata tokenIds) external {
        uint256 length = tokenIds.length;
        require(length <= BATCH_LIMIT, "MemeTokenChildTunnel: EXCEEDS_BATCH_LIMIT");
        bytes[] memory metadata = new bytes[](length);

        // withdraw tokens
        for (uint256 i = 0; i < length; i++) {
            metadata[i] = childToken.encodeTokenMetadata(tokenIds[i]);
            childToken.burn(tokenIds[i]);
        }

        // send message to root regarding token burn so it allows tokens transfer
        _sendMessageToRoot(abi.encode(msg.sender, BATCH, abi.encode(tokenIds, metadata)));
    }

    function _processMessageFromRoot(uint256 /* stateId */, address sender, bytes memory data) internal override validateSender(sender) {
        (address to, bytes4 depositType, bytes memory depositData) = abi.decode(data, (address, bytes4, bytes));
        if (depositType == SINGLE) {
            (uint256 tokenId, bytes memory metadata) = abi.decode(depositData, (uint256, bytes));
            // mint the same token which have been locked in root
            childToken.mint(to, tokenId, metadata);
        } else if (depositType == BATCH) {
            (uint256[] memory tokenIds, bytes[] memory metadatas) = abi.decode(depositData, (uint256[], bytes[]));
            for (uint i = 0; i < tokenIds.length; i++) {
                // mint the same token which have been locked in root
                childToken.mint(to, tokenIds[i], metadatas[i]);
            }
        } else {
            revert("MemeTokenChildTunnel: INVALID_DEPOSIT_TYPE");
        }
    }
}
