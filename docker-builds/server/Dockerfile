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
RUN lein cljsbuild once "server"


FROM node:11.14.0-stretch-slim
WORKDIR /memefactory
# # twitter-bot needs to be able to write here
RUN mkdir /tmp/memefactory

# Python dependencies
RUN apt-get update -y \
    && apt-get install --no-install-recommends -y \
    -q python-pip python-setuptools
RUN pip install cryptography base58

# # ENV variables
ENV CONFIG /configs/meme.config.edn

# # get compiled JS
COPY --from=build_stage /build/server /memefactory/server
COPY --from=build_stage /build/node_modules /memefactory/node_modules
COPY --from=build_stage /build/resources /memefactory/resources

# ### --- DEBUG
RUN ls /memefactory/
RUN ls /memefactory/server/

ENTRYPOINT ["node", "server/memefactory.js"]
CMD ["--max-old-space-size=2048"]
