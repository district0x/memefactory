# VARS
PROJECT_NAME="memefactory"
BUILD_ENV="dev"
DEV_IMAGE="memefactory_base:latest"
COMMIT_ID = $(shell git rev-parse --short HEAD)
DOCKER_VOL_PARAMS = "-v ${PWD}:/build/ -v vol_target_dir:/build/target -v vol_m2_cache:/root/.m2 -v vol_node_modules:/build/node_modules --workdir /build -e BUILD_ENV=${BUILD_ENV} -e MEMEFACTORY_ENV=${BUILD_ENV}"
DOCKER_NET_PARAMS = --network=${PROJECT_NAME}_dev_network
SHELL=bash
.PHONY: help

# check which registry we use
SHELL  := env DOCKER_REGISTRY=$(DOCKER_REGISTRY) $(SHELL)
DOCKER_REGISTRY ?= "district0x"

# HELP
help: ## Print help
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

.DEFAULT_GOAL := help

# DOCKER BUILDS
build-images: ## Build all containers in docker-compose file
	DOCKER_BUILDKIT=1 docker-compose -p ${PROJECT_NAME} build --parallel

build-images-no-cache: # Build base docker image with node11.14, yarn, clojure, lein, truffle
	DOCKER_BUILDKIT=1 docker-compose -p ${PROJECT_NAME} build --parallel --pull --no-cache

build-server:  ## Build server container
	DOCKER_BUILDKIT=1 docker-compose -p ${PROJECT_NAME} build --parallel server

build-ui:## Build ui container (needs server first)
	DOCKER_BUILDKIT=1 docker-compose -p ${PROJECT_NAME} build --parallel ui

build-dev-image: # Build base docker image with node11.14, yarn, clojure, lein, truffle
	docker build -t ${DEV_IMAGE} -f docker-builds/base/Dockerfile .

build-dev-no-cache: # Fully rebuild base docker image with node11.14, yarn, clojure, lein, truffle
	docker build -t ${DEV_IMAGE} -f docker-builds/base/Dockerfile . --pull --no-cache

# RUN CONTAINERS
start-containers: dev-image ## Build and start containers ((ipfs, ganache, dev container)
	docker-compose -p ${PROJECT_NAME} up -d

run-dev-shell: ## Start container in interactive mode
	docker run -ti --rm --entrypoint="" ${DOCKER_NET_PARAMS} ${DOCKER_VOL_PARAMS} ${DEV_IMAGE} bash

check-containers: ## Show docker-compose ps for given project
	docker-compose -p ${PROJECT_NAME} ps

clear-all: ## Remove containers, networks and volumes
	docker-compose -p ${PROJECT_NAME} down

# RUN TESTS using DEV_IMAGE

compile-tests: build-dev-image  ## Run tests in base container
	docker run -t --rm ${DOCKER_NET_PARAMS} ${DOCKER_VOL_PARAMS} ${DEV_IMAGE} bash -c "lein build-tests"

server-test: compile-tests  ## Run tests in base container
	docker run -t --rm ${DOCKER_NET_PARAMS} ${DOCKER_VOL_PARAMS} ${DEV_IMAGE} bash -c "lein build-tests && lein server-test"


# MANAGE DEPENDENCIES
deps: build-dev-image ## Install/update deps
	docker run -t --rm ${DOCKER_NET_PARAMS} ${DOCKER_VOL_PARAMS} ${DEV_IMAGE} bash -c "yarn deps"

clear-volumes: ## remove node_moules folder
	docker volume rm memefactory_vol_tests memefactory_vol_node_modules memefactory_vol_m2_cache memefactory_vol_target_dir memefactory_vol_ipfs_data || true

lint: deps ## Run lint
	docker run -t --rm ${DOCKER_NET_PARAMS} ${DOCKER_VOL_PARAMS} ${DEV_IMAGE} bash -c "yarn lint"


# SHORTCUTS
ui: build-ui ## Build ui container (alias for build-ui)
server: build-server ## Build server container (alias for build-server)
docker: build-images ## Build all containers (alias for docker-build)
build: build-images ## Build all containers (alias for docker-build)
up: docker-compose-up ## Start dev environment
