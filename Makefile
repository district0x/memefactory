.PHONY: help

# HELP
help: ## Print help
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

.DEFAULT_GOAL := help

# VARS
APP_NAME="memefactory"
build_environment="dev"

# DOCKER TASKS

build-images: build-server build-ui ## Build all containers

build-server: ## Build server container
	docker build -t $(APP_NAME)-server -f docker-builds/server/Dockerfile .

build-ui:## Build ui container (needs server first)
	docker build -t $(APP_NAME)-ui -f docker-builds/ui/Dockerfile .

# RUN CONTAINERS

start: ## Start containers
	echo "TODO: running docker compose"

test: ## Start containers and run unit tests
	echo "TODO: running unit tests"

deps: ## Install/update deps
	docker run -ti --rm -v ${PWD}:/build/ --workdir /build node:11.14.0-stretch sh -c "yarn deps"

lint: deps ## Run lint
	docker run -ti --rm -v ${PWD}:/build/ --workdir /build node:11.14.0-stretch sh -c "yarn lint"

# SHORTCUTS
ui: build-ui ## Build ui container (alias for build-ui)
server: build-server ## Build server container (alias for build-server)
docker: build-images ## Build all containers (alias for docker-build)
build: build-images ## Build all containers (alias for docker-build)
