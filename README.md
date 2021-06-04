<div align="left">
    <a href="https://discord.com/invite/sS2AWYm"><img alt="District0x Discord server" src="https://img.shields.io/discord/356854079022039062?label=district0x&logo=discord"></a>
    <a href="LICENSE"><img alt="LICENSE" src="https://img.shields.io/github/license/district0x/memefactory"></a>
    <a href="http://makeapullrequest.com"><img alt="pull requests welcome" src="https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat"></a></p>
</div>

# [MemeFactory](https://memefactory.io)

MemeFactory is a platform for the decentralized creation, issuance, and exchange of provably rare digital collectibles on the Ethereum blockchain. Meme Factory strives to be the first self-governing digital collectibles marketplace. By utilizing a “token curated registry”, called the Dank Registry, Meme Factory users will be able to submit original memes to a community run list, where token holders of a newly minted token, the DANK token, decide what makes the cut.

## How it Works

The easiest way to explain how Meme Factory works is from the perspective of each kind of user - creators, curators, and collectors. It’s important to note that users can be any or all of the following roles at the same time, they’re not locked in to a single one.

### Creators

Creators are the most familiar of all the roles. Their job is simple - they bring memes to the Dank Registry in hopes of listing them for sale. Valid submissions to the registry must include: the meme itself, a starting price (in ETH) for the auction, the total supply of memes issued for sale, along with a fixed amount of DANK. By requiring this deposit, low quality submissions and spam are easily avoided. Creators with genuinely dank memes are incentivized to risk DANK deposits in return for the opportunity to sell the newly issued memes for ETH. Their success is measured in the number of memes they sell, or the total amount they sell them for.

### Curators

Curators run the DANK registry, and can be split into two subcategories: challengers and voters. A challenger’s job is to find recent submissions from creators that are disagreeable for some reason (meme isn’t funny, title or tags are inappropriate, etc.). They can then initiate challenge to any submissions inclusion in the registry by depositing an equivalent fixed amount of DANK as the creator. This places their DANK at risk.

When a challenge is initiated, a voting period begins. This is where the voters come in. Using their DANK tokens, voters can privately signal their DANK balance either in favor of or against the challenge. At the end of the voting period, all votes are made public and voters receive their DANK in return.

If the challenge fails, the meme remains in the registry, and moves on to the initial meme offering. Additionally, the challenger’s DANK deposit is dispensed as a reward to winning voters. Vice versa, if the challenge succeeds, the meme is rejected from the registry, and the creator’s DANK is forfeited as a reward to both the challenger and winning voters.

In this way, challengers are incentivized to make only prudent challenges, else they risk their initial deposit by needlessly challenging an obviously popular meme or spamming challenges across all new submissions. Voters risk nothing, and are incentivized to vote as often as possible and only in their best interest - which means voting the way they _think_ will be the most popular with others. These incentive alignments together are what produce the “curation” powers of Meme Factory. Popular content churns to the top.

### Collectors

Collectors are a class completely on their own - they deal only with the aftermath of the creator’s and the curator’s actions. When a meme has secured a spot in the registry (either successfully exiting a challenge, or lasting the entire challenge period unchallenged) the Initial Meme Offering begins. Using the initial price and total supply provided by the creator, the Meme is offered for sale and begins decaying down to a lower and lower price over time, until either supply runs out or the offering ends.

During this time, any collector visiting the site, even someone who has never owned DANK, can purchase a meme using ETH. Just like a cryptokitty, these memes will themselves be tradable and resellable on a secondary market provided on Meme Factory. For any given meme that makes it through the Dank registry, there will only ever be a finite number issued. These provably rare memes will exist indefinitely on the blockchain, and can be accessed via smart contract even if Meme Factory disappeared altogether. This gives as strong an incentive to collect as any.

## Technical Overview

Following section is written for developers working on MF or related applications. Programming knowledge is assumed.

To be able to easily pick up MF stack, one should be familiar with following topics:

-   [Clojurescript](https://clojurescript.org/)
-   [d0x-INFRA](https://github.com/district0x/d0x-INFRA), [re-mount](https://github.com/district0x/d0x-INFRA/blob/master/re-mount.md)
-   [Solidity](http://solidity.readthedocs.io/en/develop/)
-   [Proxy Contracts in Solidity](https://blog.zeppelin.solutions/proxy-libraries-in-solidity-79fbe4b970fd)
-   [Token Curated Registries (TCR)](https://medium.com/@ilovebagels/token-curated-registries-1-0-61a232f8dac7)
-   [0x Protocol](https://blog.0xproject.com/a-beginners-guide-to-0x-81d30298a5e0)
-   [GraphQL](https://graphql.org/)

### Smart Contracts

MemeFactory smart-contracts are mostly an implementation of TCR. In our own implementation, each registry entry is deployed as separate contract via a factory contract. This model provides us with straightforward path to add new features for future registry entries, while immutability guarantees of existing ones are preserved. Registry entry contract is not deployed each time as a full copy of source code, but only as forwarder proxy contract. This is to ensure gas efficiency. Registry contract groups together events of all registry entries, so MF server can listen to just single contract to keep up with all updates throughout all registry entries.

Following diagram shows high-level overview of smart-contracts. Each Solidity file contains detailed comments, proceed [there](https://github.com/district0x/memefactory/tree/master/resources/public/contracts/src) to study further.

![MemeFactory SmartContracts](https://user-images.githubusercontent.com/3857155/36697475-00a2fc00-1afc-11e8-9b72-9a308d6e85d6.png)

### Server

Server part of MF is written in ClojureScript compiled into Node.js. It consists of several "microservices" provided as [mount](https://github.com/tolitius/mount) modules:

##### Deployment

Server is be able to deploy all smart-contracts and set up initial properties "in one go".

##### Data Generation

Server is be able to generate mock data by sending transactions into blockchain.

##### Syncing

Server listens to MF-related blockchain events and reconstruct SQL database using data on blockchain & IPFS.

##### GraphQL Server

Server provides data stored in SQL database via GraphQL protocol.

##### Email Notifications

Server listens to MF-related blockchain events and sends email notifications to subscribed users.

### UI

UI part of MF is written in ClojurScript, using these most notable technologies:

-   [re-frame](https://github.com/Day8/re-frame) as main web application framework
-   [re-mount](https://github.com/district0x/d0x-INFRA/blob/master/re-mount.md) modularisation pattern
-   [district-ui-modules](https://github.com/search?q=topic%3Adistrict-ui-module+org%3Adistrict0x&type=Repositories) to provide general purpose features for decentralised applications
-   [ReactSemanticUI](https://react.semantic-ui.com/introduction) for simpler styling of components
-   [GraphQL](https://graphql.org/) for communication between server and client
-   [CSS Grid Layout](https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Grid_Layout) as layout systen

### Styling

Try keeping classes at bay by using noun-like definitions:

```
[:div.meme-card]
[:div.nav-link.active]
```

Here we have 'div.meme-card' as a single "type" identificator, with optional status class. A necessary and sufficient to be identified in CSS. This should prevent styling concerns leaking into the code and avoiding "clash of classes" in responsive design.

Example of adjective-like classes to avoid:

```clojurescript
[:div.meme.padded.small.elevated]
```

Adjective-like "classes" are perfectly good as a class mixins in the styles layer of the applications:

```clojure
[:.meme
  (merge
    (box-rounded 5)
    {:background "color"})]
```

## Development

Compile contracts (assumes you have `solc` installed):

```bash
lein solc
```

Auto compile contracts on changes:

```bash
lein solc auto
```

Start ipfs:

```
ipfs daemon
```

Start server:

```bash
ganache-cli -p 8549 -l 8000000 -d -m district0x
lein repl
(start-server!)
node dev-server/memefactory.js
```

Deploy smart contracts:

```bash
truffle migrate --network ganache --reset --from 1 --to 3
```

Start UI:

```bash
lein repl
(start-ui!)
# go to http://localhost:4598/
```

Start tests:

```bash
ganache-cli -p 8549
lein test-dev
```

### Docker builds

CI deploys the so-called nightly builds on every succesfull commit to the `master` branch.
These builds target our QA (ropsten) environment, where a `watchtower` service watches and re-deploys images tagged as `latest`.
These images are versioned based on the corresponding github commit hash.

You can also build these services locally:

=SERVER=
`docker build -t district0x/memefactory-server -f docker-builds/server/Dockerfile .`

=UI=
`docker build -t district0x/memefactory-ui -f docker-builds/ui/Dockerfile .`

### QA and Production UI builds

For qa and production builds all you need to do is set
`MEMEFACTORY_ENV` appropriately. For example:

```bash
MEMEFACTORY_ENV=qa lein cljsbuild once "ui"
```

or

```bash
MEMEFACTORY_ENV=prod lein cljsbuild once "ui"
```

will build for the appropriate environment.

### Dank Faucet

Naviagting to `/#/get-dank/index` allows you to verify your phone
number and receive an initial allotment of DANK. Using the feature is
pretty easy and straight forward, however maintaining it and deploying
it are not. To get the feature working locally you need the following
pieces:

-   The Ethereum Oraclize Bridge: https://github.com/oraclize/ethereum-bridge
    -   [dev] Update the DankFaucet contract OAR according to the instructions
        in the contract and what you see when running the bridge.
    -   [prod]
        -   Uncomment the line below the comment:
            -   `// Uncomment this line when deploying to production`
        -   Comment out the line below the comment:
            -   `// Comment out this line when deploying to production`
-   Fund the contract
    -   Call `sendEth()` in the DankFaucet contract and give it some ETH.
        -   ETH is necessary to pay for Oraclize's services.
-   The Python encryption script
    -   The script lives at `scripts/encrypted_queries_tools.py`
    -   It requires Python 2.x
-   The Twilio API
    -   We use the Twilio API for phone number verification
    -   Replace the key in the line `:twilio-api-key "PUT_THE_REAL_KEY_HERE"` in:
        -   `server/core.cljs`
        -   `server/dev.cljs`

Once these things are done and you either have a Twilio test account or buy a
number and put funds in an account the phone number verification service should
work for you.
