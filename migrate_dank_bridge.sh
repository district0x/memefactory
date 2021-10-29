#!/bin/bash

truffle migrate --reset --f 14 --to 14 --network root && \
truffle migrate --reset --f 15 --to 15 --network bor --compile-none && \
truffle migrate --reset --f 16 --to 16 --network root --compile-none && \
truffle migrate --reset --f 17 --to 17 --network bor --compile-none
