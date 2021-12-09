// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import { FxBaseRootTunnelInitilializable } from "./tunnel/FxBaseRootTunnelInitializable.sol";
import { MemeToken } from "../MemeToken.sol";
import "@openzeppelin/contracts-upgradeable/proxy/utils/UUPSUpgradeable.sol";
import "@openzeppelin/contracts-upgradeable/access/OwnableUpgradeable.sol";
import "@openzeppelin/contracts/token/ERC721/IERC721Receiver.sol";

/**
 * @title MemeTokenRootTunnel
 */
contract MemeTokenRootTunnel is FxBaseRootTunnelInitilializable, IERC721Receiver, UUPSUpgradeable, OwnableUpgradeable {
    event FxWithdrawERC721(address indexed userAddress, uint256 id);
    event FxWithdrawERC721Batch(address indexed userAddress, uint256[] ids);
    event FxDepositERC721(address indexed depositor, address indexed userAddress, uint256 id);
    event FxDepositERC721Batch(address indexed depositor, address indexed userAddress, uint256[] ids);

    bytes4 public constant SINGLE = bytes4(keccak256("SINGLE"));
    bytes4 public constant BATCH = bytes4(keccak256("BATCH"));
    uint256 public constant BATCH_LIMIT = 20;

    MemeToken public rootToken;

    function initialize(address _checkpointManager, address _fxRoot, address _rootToken) initializer public {
        __Ownable_init();
        __FxBaseRootTunnel_init(_checkpointManager, _fxRoot);
        rootToken = MemeToken(_rootToken);
    }

    function _authorizeUpgrade(address newImplementation) internal override onlyOwner
    {}

    function deposit(address user, uint256 tokenId) external {
        // transfer from depositor to this contract
        rootToken.transferFrom(
            msg.sender,    // depositor
            address(this), // manager contract
            tokenId
        );

        bytes memory message = abi.encode(user, SINGLE, abi.encode(tokenId, rootToken.encodeTokenMetadata(tokenId)));
        _sendMessageToChild(message);
        emit FxDepositERC721(msg.sender, user, tokenId);
    }

    function depositBatch(address user, uint256[] calldata tokenIds) external {
        uint256 length = tokenIds.length;
        require(length <= BATCH_LIMIT, "MemeTokenRootTunnel: EXCEEDS_BATCH_LIMIT");
        bytes[] memory metadatas = new bytes[](length);
        for (uint256 i = 0; i < length; i++) {
            // transfer from depositor to this contract
            rootToken.transferFrom(
                msg.sender,    // depositor
                address(this), // manager contract
                tokenIds[i]
            );
            metadatas[i] = rootToken.encodeTokenMetadata(tokenIds[i]);
        }
        bytes memory message = abi.encode(user, BATCH, abi.encode(tokenIds, metadatas));
        _sendMessageToChild(message);
        emit FxDepositERC721Batch(msg.sender, user, tokenIds);
    }

    function onERC721Received(address, address from, uint256 tokenId, bytes calldata) external override returns (bytes4) {
        bytes memory message = abi.encode(from, SINGLE, abi.encode(tokenId, rootToken.encodeTokenMetadata(tokenId)));
        _sendMessageToChild(message);
        emit FxDepositERC721(from, from, tokenId);
        return IERC721Receiver.onERC721Received.selector;
    }

    // exit processor
    function _processMessageFromChild(bytes memory data) internal override {
        (address to, bytes4 withdrawType, bytes memory withdrawData) = abi.decode(data, (address, bytes4, bytes));
        if (withdrawType == SINGLE) {
            (uint256 tokenId, bytes memory metaData) = abi.decode(withdrawData, (uint256, bytes));
            // Note: If your token is only minted in L2, you can exit
            // it with metadata. But if it was minted on L1, it'll be
            // simply transferred to withdrawer address.
            if (rootToken.exists(tokenId)) {
                rootToken.safeTransferFrom(address(this), to, tokenId);
            } else {
                rootToken.mint(to, tokenId, metaData);
            }
            emit FxWithdrawERC721(to, tokenId);
        } else if (withdrawType == BATCH) {
            (uint256[] memory tokenIds, bytes[] memory metaDatas) = abi.decode(withdrawData, (uint256[], bytes[]));
            for (uint i = 0; i < tokenIds.length; i++) {
                if (rootToken.exists(tokenIds[i])) {
                    rootToken.safeTransferFrom(address(this), to, tokenIds[i]);
                } else {
                    rootToken.mint(to, tokenIds[i], metaDatas[i]);
                }
            }
            emit FxWithdrawERC721Batch(to, tokenIds);
        } else {
            revert("MemeTokenRootTunnel: INVALID_WITHDRAW_TYPE");
        }
    }
}
