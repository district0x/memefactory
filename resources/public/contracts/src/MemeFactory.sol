pragma solidity ^0.4.18;

import "Registry.sol";
import "Meme.sol";
import "forwarder/Forwarder.sol";
import "token/minime/MiniMeToken.sol";

contract MemeFactory is ApproveAndCallFallBack {
  Registry public registry;
  MiniMeToken public registryToken;
  bytes32 public constant depositKey = sha3("deposit");
  uint public constant version = 1;

  function MemeFactory(Registry _registry, MiniMeToken _registryToken) {
    registry = _registry;
    registryToken = _registryToken;
  }

  function createMeme(
    address _creator,
    string _name,
    bytes _imageHash,
    bytes _metaHash,
    uint _totalSupply,
    uint _startPrice
  )
  public
  {
    uint deposit = registry.db().getUIntValue(depositKey);
    Meme meme = Meme(new Forwarder());
    require(registryToken.transferFrom(_creator, this, deposit));
    require(registryToken.approve(meme, deposit));

    meme.construct(
      version,
      msg.sender,
      _name,
      _imageHash,
      _metaHash,
      _totalSupply,
      _startPrice
    );

    registry.addRegistryEntry(meme);
    registry.fireRegistryEntryEvent(meme, "added", version, new uint[](0));
  }

  function receiveApproval(
    address from,
    uint256 _amount,
    address _token,
    bytes _data)
  public
  {
    require(_token == address(registryToken));
    require(this.call(_data));
  }
}