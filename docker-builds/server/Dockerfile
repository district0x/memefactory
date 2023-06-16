FROM memefactory_base:local AS build_stage

ARG BUILD_ENV=prod
ENV BUILD_ENV=${BUILD_ENV}
ENV MEMEFACTORY_ENV=${BUILD_ENV}
ENV PYTHON=/usr/bin/python
ENV npm_config_user=root
ENV SMART_CONTRACTS=./src/memefactory/shared/smart_contracts_${BUILD_ENV}.cljs
ENV SMART_CONTRACTS_BUILD_PATH=./resources/public/contracts/build/

RUN mkdir -p /root/.config/truffle/

COPY  . /build/
WORKDIR /build

RUN lein deps
RUN yarn deps
RUN truffle compile
RUN lein cljsbuild once "server"


FROM memefactory_base:local

ARG BUILD_ENV=qa

ENV BUILD_ENV=${BUILD_ENV}
ENV MEMEFACTORY_ENV=${BUILD_ENV}
ENV CONFIG /configs/meme.config.edn

# WORKDIR /memefactory

# # # twitter-bot needs to be able to write here
# Python dependencies
RUN apt-get update && apt-get upgrade -yqq \
    && apt-get install --no-install-recommends -yq python-pip python-setuptools \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /tmp/memefactory

RUN pip2 install --no-cache-dir cryptography base58

# # get compiled JS
COPY --from=build_stage /build/server /memefactory/server
COPY --from=build_stage /build/node_modules /memefactory/node_modules
COPY --from=build_stage /build/resources /memefactory/resources

ENTRYPOINT ["node", "server/memefactory.js"]
CMD ["--max-old-space-size=2048"]
