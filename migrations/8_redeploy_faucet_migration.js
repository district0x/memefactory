const DankToken = artifacts.require("DankToken");
const DankFaucet = artifacts.require ("DankFaucet");

// TODO : adjust these
const dankTokenAddress = "0x4356ac01d00d3c5f689b93b8f6ea01a65973ed26";
const dankFaucetAddress = "0x90920ee8ed813dbb4d78cecae87db75280ed0faf";

module.exports = function(deployer, network, accounts) {

  const address = accounts [0];
  const gas = 4e6;
  const opts = {gas: gas, from: address};

  deployer.then (() => {
    console.log ("@@@ using Web3 version:", web3.version.api);
    console.log ("@@@ using address", address);
  })

  // TODO
  // get eth and dank balance
    .then (() => Promise.all ([
      DankFaucet.at (dankFaucetAddress),
      DankToken.at (dankTokenAddress)
    ]))
    .then (([
      dankFaucet,
      dankToken
    ]) => Promise.all ([
      web3.eth.getBalance(dankFaucetAddress),
      dankToken.balanceOf (dankFaucetAddress)
    ]))
    .then (([
      ethBalance,
      dankBalance
    ]) => {
      console.log ("@@@ Faucet ETH balance: ", ethBalance);
      console.log ("@@@ Faucet DANK balance: ", dankBalance);
    })

  // drain eth and dank
    .then (() => DankFaucet.at (dankFaucetAddress))
    .then ((dankFaucet) => Promise.all ([
      dankFaucet.withdrawEth (Object.assign(opts, {gas: 100000})),
      dankFaucet.withdrawDank (Object.assign(opts, {gas: 200000}))
    ]))
    .then (([
      tx1,
      tx2
    ]) => console.log ("@@@ Faucet succesfully drained of all funds"))

  // redeploy
  // seed with the same eth and dank

  ;
}
