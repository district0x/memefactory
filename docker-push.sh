#!/bin/bash

#--- FUNCTIONS

function build {
  {
    NAME=$1
    TAG=$(git log -1 --pretty=%h)
    IMG=$NAME:$TAG
    LATEST=$NAME:latest
    SERVICE=$(echo $NAME | cut -d "-" -f 2)

    echo "=============================="
    echo  "["$SERVICE"] Buidling: "$IMG""
    echo "=============================="

    case $SERVICE in
      "ui")
        lein garden once
        env MEMEFACTORY_ENV=qa lein cljsbuild once "ui"
        ;;
      "server")
        lein cljsbuild once "server"
        ;;
      *)
        echo "ERROR: don't know what to do with SERVICE: "$SERVICE""
        exit 1
        ;;
    esac

    docker build -t $IMG -f docker-builds/$SERVICE/Dockerfile .
    docker tag $IMG $LATEST
  } || {
    echo "EXCEPTION WHEN BUIDLING "$IMG""
    exit 1
  }
}

function push {
  NAME=$1
  echo "Pushing: " $NAME
  docker push $NAME
}

function login {
  echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
}

function before {
  lein deps
  lein npm install
  lein solc once
}

#--- EXECUTE

before
login

images=(
  district0x/memefactory-server
  district0x/memefactory-ui
)

for i in "${images[@]}"; do
  (
     build $i    
    push $i
  )

done # END: i loop

exit $?
