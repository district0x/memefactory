pragma solidity ^0.4.24;

import "auth/DSAuth.sol";
import "db/EternalDb.sol";
import "proxy/MutableForwarder.sol"; // Keep it included despite not being used (for compiler)

/**
 * @title Central contract for TCR registry
 *
 * @dev Manages state about deployed registry entries and factories
 * Serves as a central point for firing all registry entry events
 * This contract is not accessed directly, but through MutableForwarder. See MutableForwarder.sol for more comments.
 */

contract Registry is DSAuth {
  address public target; // Keep it here, because this contract is deployed as MutableForwarder

  event MemeConstructedEvent(uint version, address creator, bytes metaHash, uint totalSupply);
  event MemeMintedEvent(uint version, address creator, uint tokenStartId, uint tokenEndId, uint totalMinted);

  event ChallengeCreatedEvent(uint version, address challenger, uint commitPeriodEnd, uint revealPeriodEnd, uint rewardPool, bytes metahash);
  event VoteCommittedEvent(uint version, address voter, uint amount);
  event VoteRevealedEvent(uint version, address voter, uint option);
  event VoteAmountClaimedEvent(uint version, address voter);
  event VoteRewardClaimedEvent(uint version, address voter, uint amount);
  event ChallengeRewardClaimedEvent(uint version, address voter, uint amount);

  event ParamChangeConstructedEvent(uint version, address creator, address db, string key, uint value);

  bytes32 public constant challengePeriodDurationKey = sha3("challengePeriodDuration");
  bytes32 public constant commitPeriodDurationKey = sha3("commitPeriodDuration");
  bytes32 public constant revealPeriodDurationKey = sha3("revealPeriodDuration");
  bytes32 public constant depositKey = sha3("deposit");
  bytes32 public constant challengeDispensationKey = sha3("challengeDispensation");
  bytes32 public constant voteQuorumKey = sha3("voteQuorum");
  bytes32 public constant maxTotalSupplyKey = sha3("maxTotalSupply");
  bytes32 public constant maxAuctionDurationKey = sha3("maxAuctionDuration");

  EternalDb public db;
  bool private wasConstructed;

  /**
   * @dev Constructor for this contract.
   * Native constructor is not used, because we use a forwarder pointing to single instance of this contract,
   * therefore constructor must be called explicitly.

   * @param _db Address of EternalDb related to this registry
   */
  function construct(EternalDb _db)
  external
  {
    require(address(_db) != 0x0, "Registry: Address can't be 0x0");

    db = _db;
    wasConstructed = true;
    owner = msg.sender;
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
    db.setBooleanValue(sha3("isFactory", _factory), _isFactory);
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
    db.setBooleanValue(sha3("isRegistryEntry", _registryEntry), true);
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

  function fireMemeConstructedEvent(uint version, address creator, bytes metaHash, uint totalSupply)
  public
  onlyRegistryEntry
  {
    emit MemeConstructedEvent(version, creator, metaHash, totalSupply);
  }

  function fireMemeMintedEvent(uint version, address creator, uint tokenStartId, uint tokenEndId, uint totalMinted)
  public
  onlyRegistryEntry
  {
    emit MemeMintedEvent(version, creator, tokenStartId, tokenEndId, totalMinted);
  }

  function fireChallengeCreatedEvent(uint version, address challenger, uint commitPeriodEnd, uint revealPeriodEnd, uint rewardPool, bytes metahash)
  public
  onlyRegistryEntry
  {
    emit ChallengeCreatedEvent(version,  challenger, commitPeriodEnd, revealPeriodEnd, rewardPool, metahash);
  }

  function fireVoteCommittedEvent(uint version, address voter, uint amount)
  public
  onlyRegistryEntry
  {
    emit VoteCommittedEvent(version, voter, amount);
  }

  function fireVoteRevealedEvent(uint version, address voter, uint option)
  public
  onlyRegistryEntry
  {
    emit VoteRevealedEvent(version, voter, option);
  }

  function fireVoteAmountClaimedEvent(uint version, address voter)
  public
  onlyRegistryEntry
  {
    emit VoteAmountClaimedEvent(version, voter);
  }

  function fireVoteRewardClaimedEvent(uint version, address voter, uint amount)
  public
  onlyRegistryEntry
  {
    emit VoteRewardClaimedEvent(version, voter, amount);
  }

  function fireChallengeRewardClaimedEvent(uint version, address voter, uint amount)
  public
  onlyRegistryEntry
  {
    emit ChallengeRewardClaimedEvent(version, voter, amount);
  }

  function fireParamChangeConstructedEvent(uint version, address creator, address db, string key, uint value)
  public
  onlyRegistryEntry
  {
    emit ParamChangeConstructedEvent(version, creator, db, key, value);
  }

  /**
   * @dev Returns whether address is valid registry entry factory

   * @return True if address is factory
   */
  function isFactory(address factory) public constant returns (bool) {
    return db.getBooleanValue(sha3("isFactory", factory));
  }

  /**
   * @dev Returns whether address is valid registry entry

   * @return True if address is registry entry
   */
  function isRegistryEntry(address registryEntry) public constant returns (bool) {
    return db.getBooleanValue(sha3("isRegistryEntry", registryEntry));
  }

  /**
   * @dev Returns whether emergency stop is happening

   * @return True if emergency is happening
   */
  function isEmergency() public constant returns (bool) {
    return db.getBooleanValue("isEmergency");
  }
}
