FROM node:11.14.0-stretch AS build_stage
ARG BUILD_ENV=prod
ENV BUILD_ENV=${BUILD_ENV}
ENV MEMEFACTORY_ENV=${BUILD_ENV}
ENV SMART_CONTRACTS=./src/memefactory/shared/smart_contracts_${BUILD_ENV}.cljs
ENV SMART_CONTRACTS_BUILD_PATH=./resources/public/contracts/build/
RUN git config --global url."https://".insteadOf git://

RUN apt-get update && apt-get install -yqq --no-install-recommends clojure
ADD https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein /usr/bin/lein
RUN chmod +x /usr/bin/lein

RUN mkdir -p -m 0600 ~/.ssh && ssh-keyscan github.com >> ~/.ssh/known_hosts
RUN mkdir -p /root/.config/truffle/

COPY  . /build/
WORKDIR /build
RUN npm install --global truffle@~5.4.0

RUN lein garden once  \
    && lein deps \
    && yarn deps

RUN truffle compile
RUN lein cljsbuild once "ui"

#########################
FROM nginx:alpine
ENV BUILD_ENV=${BUILD_ENV}
ENV MEMEFACTORY_ENV=${BUILD_ENV}

# replace nginx config
# COPY docker-builds/ui/nginx.conf /etc/nginx/nginx.conf

# replace default server
COPY docker-builds/ui/default /etc/nginx/conf.d/default.conf

# get compiled JS
COPY --from=build_stage  /build/resources/public /memefactory/resources/public/
RUN ls /memefactory/resources/public/

EXPOSE 80
