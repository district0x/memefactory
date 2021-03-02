ARG BUILD_ENV=qa

FROM node:11.14.0-stretch AS build_stage

ENV BUILD_ENV=${BUILD_ENV}
ENV MEMEFACTORY_ENV=${BUILD_ENV}
ENV SMART_CONTRACTS=./src/memefactory/shared/smart_contracts_${BUILD_ENV}.cljs
ENV SMART_CONTRACTS_BUILD_PATH=./resources/public/contracts/build/

COPY  . /build/
WORKDIR /build

RUN apt-get update && apt-get install -yqq --no-install-recommends clojure
ADD https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein /usr/bin/lein
RUN chmod +x /usr/bin/lein

RUN mkdir -p -m 0600 ~/.ssh && ssh-keyscan github.com >> ~/.ssh/known_hosts
RUN mkdir -p /root/.config/truffle/
RUN npm install --global truffle yarn


RUN lein deps
RUN yarn deps
RUN truffle compile


FROM prerendercloud/webserver:latest
MAINTAINER "Filip Bielejec" <filip@district0x.io>

# for debugging
RUN apt-get install --no-install-recommends -y -q nano

# get compiled JS
COPY --from=build_stage resources/public /wwwroot

# setup redirects
COPY docker-builds/ui/_redirects /app/_redirects

EXPOSE 9000
