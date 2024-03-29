// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "./auth/DSAuth.sol";
import "./db/EternalDb.sol";
import "./proxy/MutableForwarder.sol"; // Keep it included despite not being used (for compiler)

/**
 * @title Central contract for TCR registry
 *
 * @dev Manages state about deployed registry entries and factories
 * Serves as a central point for firing all registry entry events
 * This contract is not accessed directly, but through MutableForwarder. See MutableForwarder.sol for more comments.
 */

contract Registry is DSAuth {
  address private dummyTarget; // Keep it here, because this contract is deployed as MutableForwarder

  bytes32 public constant challengePeriodDurationKey = keccak256("challengePeriodDuration");
  bytes32 public constant commitPeriodDurationKey = keccak256("commitPeriodDuration");
  bytes32 public constant revealPeriodDurationKey = keccak256("revealPeriodDuration");
  bytes32 public constant depositKey = keccak256("deposit");
  bytes32 public constant challengeDispensationKey = keccak256("challengeDispensation");
  bytes32 public constant maxTotalSupplyKey = keccak256("maxTotalSupply");
  bytes32 public constant maxAuctionDurationKey = keccak256("maxAuctionDuration");

  event MemeConstructedEvent(address registryEntry, uint version, address creator, bytes metaHash, uint totalSupply, uint deposit, uint challengePeriodEnd);
  event MemeMintedEvent(address registryEntry, uint version, address creator, uint tokenStartId, uint tokenEndId, uint totalMinted);

  event ChallengeCreatedEvent(address registryEntry, uint version, address challenger, uint commitPeriodEnd, uint revealPeriodEnd, uint rewardPool, bytes metahash);
  event VoteCommittedEvent(address registryEntry, uint version, address voter, uint amount);
  event VoteRevealedEvent(address registryEntry, uint version, address voter, uint option);
  event VoteAmountClaimedEvent(address registryEntry, uint version, address voter);
  event VoteRewardClaimedEvent(address registryEntry, uint version, address voter, uint amount);
  event ChallengeRewardClaimedEvent(address registryEntry, uint version, address challenger, uint amount);

  event ParamChangeConstructedEvent(address registryEntry, uint version, address creator, address db, string key, uint value, uint deposit, uint challengePeriodEnd, bytes metaHash);
  event ParamChangeAppliedEvent(address registryEntry, uint version);

  EternalDb public db;
  bool private wasConstructed;
  // nextTokenId is used to indicate the tokenId to use for the next brand-new minted token.
  uint public nextTokenId;

  /**
   * @dev Constructor for this contract.
   * Native constructor is not used, because we use a forwarder pointing to single instance of this contract,
   * therefore constructor must be called explicitly.

   * @param _db Address of EternalDb related to this registry
   */
  function construct(EternalDb _db)
  external
  {
    require(address(_db) != address(0), "Registry: Address can't be 0x0");
    require(!wasConstructed);

    db = _db;
    wasConstructed = true;
    owner = msg.sender;

    // Each chain should have their own token id range so they do not collide when porting memes between chains
    // so the first 2^64 bits of the tokenId is the chain identifier, andthe rest 2^192 are for the actual tokenIds
    //  within each chain
    nextTokenId = block.chainid << 192;
  }

  modifier onlyFactory() {
    require(isFactory(msg.sender), "Registry: Sender should be factory");
    _;
  }

  modifier onlyRegistryEntry() {
    require(isRegistryEntry(msg.sender), "Registry: Sender should registry entry");
    _;
  }

  modifier notEmergency() {
    require(!isEmergency(),"Registry: Emergency mode is enable");
    _;
  }

  /**
   * @dev Sets whether address is factory allowed to add registry entries into registry
   * Must be callable only by authenticated user

   * @param _factory Address of a factory contract
   * @param _isFactory Whether the address is allowed factory
   */
  function setFactory(address _factory, bool _isFactory)
  external
  auth
  {
    db.setBooleanValue(keccak256(abi.encodePacked("isFactory", _factory)), _isFactory);
  }

  /**
   * @dev Adds address as valid registry entry into the Registry
   * Must be callable only by allowed factory contract

   * @param _registryEntry Address of new registry entry
   */
  function addRegistryEntry(address _registryEntry)
  external
  onlyFactory
  notEmergency
  {
    db.setBooleanValue(keccak256(abi.encodePacked("isRegistryEntry", _registryEntry)), true);
  }

  /**
   * @dev Sets emergency state to pause all trading operations
   * Must be callable only by authenticated user

   * @param _isEmergency True if emergency is happening
   */
  function setEmergency(bool _isEmergency)
  external
  auth
  {
    db.setBooleanValue("isEmergency", _isEmergency);
  }

  function fireMemeConstructedEvent(uint version, address creator, bytes memory metaHash, uint totalSupply, uint deposit, uint challengePeriodEnd)
  public
  onlyRegistryEntry
  {
    emit MemeConstructedEvent(msg.sender, version, creator, metaHash, totalSupply, deposit, challengePeriodEnd);
  }

  function fireMemeMintedEvent(uint version, address creator, uint tokenStartId, uint tokenEndId, uint totalMinted)
  public
  onlyRegistryEntry
  {
    // keep tracks of the total minted token to know what should be the next tokenId
    nextTokenId += totalMinted;
    emit MemeMintedEvent(msg.sender, version, creator, tokenStartId, tokenEndId, totalMinted);
  }

  function fireChallengeCreatedEvent(uint version, address challenger, uint commitPeriodEnd, uint revealPeriodEnd, uint rewardPool, bytes memory metahash)
  public
  onlyRegistryEntry
  {
    emit ChallengeCreatedEvent(msg.sender, version,  challenger, commitPeriodEnd, revealPeriodEnd, rewardPool, metahash);
  }

  function fireVoteCommittedEvent(uint version, address voter, uint amount)
  public
  onlyRegistryEntry
  {
    emit VoteCommittedEvent(msg.sender, version, voter, amount);
  }

  function fireVoteRevealedEvent(uint version, address voter, uint option)
  public
  onlyRegistryEntry
  {
    emit VoteRevealedEvent(msg.sender, version, voter, option);
  }

  function fireVoteAmountClaimedEvent(uint version, address voter)
  public
  onlyRegistryEntry
  {
    emit VoteAmountClaimedEvent(msg.sender, version, voter);
  }

  function fireVoteRewardClaimedEvent(uint version, address voter, uint amount)
  public
  onlyRegistryEntry
  {
    emit VoteRewardClaimedEvent(msg.sender, version, voter, amount);
  }

  function fireChallengeRewardClaimedEvent(uint version, address challenger, uint amount)
  public
  onlyRegistryEntry
  {
    emit ChallengeRewardClaimedEvent(msg.sender, version, challenger, amount);
  }

  function fireParamChangeConstructedEvent(uint version, address creator, address db, string memory key, uint value, uint deposit, uint challengePeriodEnd, bytes memory metaHash)
  public
  onlyRegistryEntry
  {
    emit ParamChangeConstructedEvent(msg.sender, version, creator, db, key, value, deposit, challengePeriodEnd, metaHash);
  }

  function fireParamChangeAppliedEvent(uint version)
  public
  onlyRegistryEntry
  {
    emit ParamChangeAppliedEvent(msg.sender, version);
  }

  /**
   * @dev Returns whether address is valid registry entry factory

   * @return True if address is factory
   */
  function isFactory(address factory) public view returns (bool) {
    return db.getBooleanValue(keccak256(abi.encodePacked("isFactory", factory)));
  }

  /**
   * @dev Returns whether address is valid registry entry

   * @return True if address is registry entry
   */
  function isRegistryEntry(address registryEntry) public view returns (bool) {
    return db.getBooleanValue(keccak256(abi.encodePacked("isRegistryEntry", registryEntry)));
  }

  /**
   * @dev Returns whether emergency stop is happening

   * @return True if emergency is happening
   */
  function isEmergency() public view returns (bool) {
    return db.getBooleanValue("isEmergency");
  }
}
