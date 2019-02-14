#!/bin/bash

function build {
  NAME=$1
  TAG=$(git log -1 --pretty=%h)
  IMG=$NAME:$TAG
  LATEST=$NAME:latest

  echo "=============================="
  echo "Buidling: " $IMG
  echo "=============================="

  docker build -t $IMG -f docker-builds/server/Dockerfile .
  docker tag $IMG $LATEST
}

function push {
  NAME=$1
  echo "Pushing: " $NAME
  docker push $NAME
}

function login {
  echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
}

login

images=(district0x/memefactory-server district0x/memefactory-ui)

for i in "${images[@]}"; do
  (
    build $i
    push $i
  )&

  # wait for all spawned sub-processes to finish
  wait

done # END: i loop

echo "=============================="
echo "DONE"
echo "=============================="

exit $?
