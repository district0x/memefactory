#!/bin/bash

#--- ARGS

BUILD_ENV=$1

#--- FUNCTIONS

function build {
  {
    NAME=$1
    BUILD_ENV=$2
    TAG=$(git log -1 --pretty=%h)
    IMG=$NAME:$TAG

    SERVICE=$(echo $NAME | cut -d "-" -f 2)

    echo "============================================="
    echo  "["$BUILD_ENV"] ["$SERVICE"] Buidling: "$IMG""
    echo "============================================="

    case $SERVICE in
      "ui")
        lein garden once
        env MEMEFACTORY_ENV=$BUILD_ENV lein cljsbuild once "ui"
        ;;
      "server")
        env MEMEFACTORY_ENV=$BUILD_ENV lein cljsbuild once "server"
        ;;
      *)
        echo "ERROR: don't know what to do with SERVICE: "$SERVICE""
        exit 1
        ;;
    esac

    docker build -t $IMG -f docker-builds/$SERVICE/Dockerfile .

    case $BUILD_ENV in
      "qa")
        # qa images are tagged as `latest`
        docker tag $IMG $NAME:latest
        ;;
      "production")
        # prod images are tagged as `release`
        docker tag $IMG $NAME:release
        ;;
      *)
        echo "ERROR: don't know what to do with BUILD_ENV: "$BUILD_ENV""
        exit 1
        ;;
    esac

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
    build $i $BUILD_ENV
    push $i
  )

done # END: i loop

exit $?
