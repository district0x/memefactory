
FROM node:11.14.0-stretch AS build_stage
# FROM node:latest AS build_stage
ARG build_environment="qa"

COPY  . /build/
WORKDIR /build

ENV BUILD_ENV=$build_environment
ENV MEMEFACTORY_ENV=$build_environment
ENV SMART_CONTRACTS="./src/memefactory/shared/smart_contracts_${BUILD_ENV}.cljs"
ENV SMART_CONTRACTS_BUILD_PATH=./resources/public/contracts/build/

RUN apt-get update && apt-get install -yqq --no-install-recommends clojure
ADD https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein /usr/bin/lein
RUN chmod +x /usr/bin/lein

RUN mkdir -p -m 0600 ~/.ssh && ssh-keyscan github.com >> ~/.ssh/known_hosts
RUN mkdir -p /root/.config/truffle/
RUN npm install --global truffle@~5.3.0

RUN lein garden once  \
    && lein deps \
    && yarn deps

RUN truffle compile
RUN lein cljsbuild once "ui"

FROM prerendercloud/webserver:latest
MAINTAINER "Filip Bielejec" <filip@district0x.io>
RUN apt-get update && apt-get upgrade -yqq \
    && apt-get clean \
 && rm -rf /var/lib/apt/lists/*

# get compiled JS
COPY --from=build_stage /build/resources/public /wwwroot

# setup redirects
COPY docker-builds/ui/_redirects /app/_redirects

EXPOSE 9000
