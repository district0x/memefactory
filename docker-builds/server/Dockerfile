FROM buildpack-deps:20.04 as nvm_base

ENV NVM_DIR /usr/local/nvm
ENV NODE_VERSION 11.14.0
ENV YARN_VERSION 1.22.19
ENV PYTHON=/usr/bin/python
ENV npm_config_user=root

RUN groupadd --gid 1000 node \
  && useradd --uid 1000 --gid node --shell /bin/bash --create-home node

RUN ARCH= && dpkgArch="$(dpkg --print-architecture)" \
  && case "${dpkgArch##*-}" in \
  amd64) ARCH='x64';; \
  arm64) ARCH='arm64';; \
  *) echo "unsupported architecture"; exit 1 ;; \
  esac \
  # gpg keys listed at https://github.com/nodejs/node#release-keys
  && set -ex \
  && for key in \
  4ED778F539E3634C779C87C6D7062848A1AB005C \
  141F07595B7B3FFE74309A937405533BE57C7D57 \
  74F12602B6F1C4E913FAA37AD3A89613643B6201 \
  DD792F5973C6DE52C432CBDAC77ABFA00DDBF2B7 \
  61FC681DFB92A079F1685E77973F295594EC4689 \
  8FCCA13FEF1D0C2E91008E09770F7A9A5AE15600 \
  C4F0DFFF4E8C1A8236409D08E73BC641CC11F4C8 \
  890C08DB8579162FEE0DF9DB8BEAB4DFCF555EF4 \
  C82FA3AE1CBEDC6BE46B9360C43CEC45C17AB93C \
  108F52B48DB57BB0CC439B2997B01419BD92F80A \
  ; do \
  gpg --batch --keyserver hkps://keys.openpgp.org --recv-keys "$key" || \
  gpg --batch --keyserver keyserver.ubuntu.com --recv-keys "$key" ; \
  done \
  && curl -fsSLO --compressed "https://nodejs.org/dist/v$NODE_VERSION/node-v$NODE_VERSION-linux-$ARCH.tar.xz" \
  && curl -fsSLO --compressed "https://nodejs.org/dist/v$NODE_VERSION/SHASUMS256.txt.asc" \
  && gpg --batch --decrypt --output SHASUMS256.txt SHASUMS256.txt.asc \
  && grep " node-v$NODE_VERSION-linux-$ARCH.tar.xz\$" SHASUMS256.txt | sha256sum -c - \
  && tar -xJf "node-v$NODE_VERSION-linux-$ARCH.tar.xz" -C /usr/local --strip-components=1 --no-same-owner \
  && rm "node-v$NODE_VERSION-linux-$ARCH.tar.xz" SHASUMS256.txt.asc SHASUMS256.txt \
  && ln -s /usr/local/bin/node /usr/local/bin/nodejs \
  && node --version \
  && npm --version

# hadolint ignore=SC2043
RUN set -ex \
  && for key in \
  6A010C5166006599AA17F08146C2130DFD2497F5 \
  ; do \
  gpg --batch --keyserver hkps://keys.openpgp.org --recv-keys "$key" || \
  gpg --batch --keyserver keyserver.ubuntu.com --recv-keys "$key" ; \
  done \
  && curl -fsSLO --compressed "https://yarnpkg.com/downloads/$YARN_VERSION/yarn-v$YARN_VERSION.tar.gz" \
  && curl -fsSLO --compressed "https://yarnpkg.com/downloads/$YARN_VERSION/yarn-v$YARN_VERSION.tar.gz.asc" \
  && gpg --batch --verify yarn-v$YARN_VERSION.tar.gz.asc yarn-v$YARN_VERSION.tar.gz \
  && mkdir -p /opt \
  && tar -xzf yarn-v$YARN_VERSION.tar.gz -C /opt/ \
  && ln -s /opt/yarn-v$YARN_VERSION/bin/yarn /usr/local/bin/yarn \
  && ln -s /opt/yarn-v$YARN_VERSION/bin/yarnpkg /usr/local/bin/yarnpkg \
  && rm yarn-v$YARN_VERSION.tar.gz.asc yarn-v$YARN_VERSION.tar.gz \
  && yarn --version

COPY docker-builds/base/docker-entrypoint.sh /usr/local/bin/
ENTRYPOINT ["docker-entrypoint.sh"]

#hadolint ignore=DL3008
RUN apt-get update \
  && apt-get upgrade -y \
  && apt-get install -yqq --no-install-recommends ca-certificates clojure git leiningen python2 \ 
  && apt-get clean \
  && rm -rf /var/lib/apt/lists/* \
  && ln -s /usr/bin/python2 /usr/bin/python
RUN git config --global url."https://".insteadOf git://
RUN mkdir -p -m 0700 ~/.ssh && ssh-keyscan github.com >> ~/.ssh/known_hosts

# hadolint ignore=SC2174
RUN npm config set user 0 && \
  npm config set unsafe-perm true && \
  npm install --global truffle@~5.4

WORKDIR /build
VOLUME [ "~/.m2" ]
VOLUME [ "/build" ]
VOLUME [ "/build/node_modules" ]

CMD [ "node" ]

FROM nvm_base AS build_stage

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
