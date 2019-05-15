#!/bin/bash

#--- ARGS

BUILD_ENV=$1

#--- FUNCTIONS

function tag {
  BUILD_ENV=$1

  case $BUILD_ENV in
    "qa")
      echo latest
      ;;
    "prod")
      echo release
      ;;
    *)
      echo "ERROR: don't know what to do with BUILD_ENV: "$BUILD_ENV""
      exit 1
      ;;
  esac
}

function build {
  {
    NAME=$1
    BUILD_ENV=$2
    VERSION=$(git log -1 --pretty=%h)
    TAG=$(tag $BUILD_ENV)
    IMG=$NAME:$VERSION

    SERVICE=$(echo $NAME | cut -d "-" -f 2)

    echo "============================================================="
    echo  "["$BUILD_ENV"] ["$SERVICE"] Buidling: "$IMG" with tag "$TAG""
    echo "============================================================="

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

    if [[ $SERVICE == "ui" && $BUILD_ENV == "qa" ]]; then
      docker build -t $IMG -f docker-builds/$SERVICE/prerender/Dockerfile .
    else
      docker build -t $IMG -f docker-builds/$SERVICE/Dockerfile .
    fi

    docker tag $IMG $NAME:$TAG

  } || {
    echo "EXCEPTION WHEN BUIDLING "$IMG""
    exit 1
  }
}

function push {
  NAME=$1
  BUILD_ENV=$2
  TAG=$(tag $BUILD_ENV)

  echo "============================================================="
  echo  "Pushing "$NAME" : "$TAG" "
  echo "============================================================="

  docker push $NAME:$TAG
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
    push $i $BUILD_ENV
  )

done # END: i loop

exit $?
