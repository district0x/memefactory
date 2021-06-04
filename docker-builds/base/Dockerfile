FROM node:11.14.0-stretch AS build_stage

RUN apt-get update \
    && apt-get upgrade -y \
    && apt-get install -yqq --no-install-recommends clojure \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

ADD https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein /usr/bin/lein
RUN chmod +x /usr/bin/lein && lein version

RUN mkdir -p -m 0600 ~/.ssh && ssh-keyscan github.com >> ~/.ssh/known_hosts
RUN mkdir -p /root/.config/truffle/ \
    && npm install --global truffle@~5.3.0

WORKDIR /build
VOLUME [ "/root/.m2" ]
VOLUME [ "/build" ]
VOLUME [ "/build/node_modules" ]
