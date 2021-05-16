FROM node:11.14.0-stretch AS build_stage
ARG BUILD_ENV=qa
ENV BUILD_ENV=${BUILD_ENV}
ENV MEMEFACTORY_ENV=${BUILD_ENV}
ENV SMART_CONTRACTS=./src/memefactory/shared/smart_contracts_${BUILD_ENV}.cljs
ENV SMART_CONTRACTS_BUILD_PATH=./resources/public/contracts/build/

RUN apt-get update && apt-get install -yqq --no-install-recommends clojure
ADD https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein /usr/bin/lein
RUN chmod +x /usr/bin/lein

RUN mkdir -p -m 0600 ~/.ssh && ssh-keyscan github.com >> ~/.ssh/known_hosts
RUN mkdir -p /root/.config/truffle/
RUN npm install --global truffle@~5.3.0

COPY  . /build/
WORKDIR /build

RUN lein deps
RUN yarn deps
RUN truffle compile
RUN lein cljsbuild once "server"


FROM node:11.14.0-stretch-slim

ARG BUILD_ENV=qa

ENV BUILD_ENV=${BUILD_ENV}
ENV MEMEFACTORY_ENV=${BUILD_ENV}
ENV CONFIG /configs/meme.config.edn

WORKDIR /memefactory

# # twitter-bot needs to be able to write here
RUN mkdir /tmp/memefactory

# Python dependencies
RUN apt-get update && apt-get upgrade -yqq \
    && apt-get install --no-install-recommends -yq python-pip python-setuptools \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

RUN pip install --no-cache-dir cryptography base58

# get compiled JS
COPY --from=build_stage /build/server /memefactory/server
COPY --from=build_stage /build/node_modules /memefactory/node_modules
COPY --from=build_stage /build/resources /memefactory/resources

ENTRYPOINT ["node", "server/memefactory.js"]
CMD ["--max-old-space-size=2048"]
