#!/bin/bash

root=${1:-root}
l2=${2:-bor}

truffle migrate --reset --f 19 --to 19 --network $root && \
truffle migrate --reset --f 20 --to 20 --network $l2 --compile-none && \
truffle migrate --reset --f 21 --to 21 --network $root --compile-none && \
truffle migrate --reset --f 22 --to 22 --network $l2 --compile-none && \
truffle migrate --reset --f 23 --to 23 --network $root --compile-none
