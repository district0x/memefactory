FROM node:11.14.0-stretch AS build_stage

RUN apt-get update \
    && apt-get install -yqq --no-install-recommends clojure

ADD https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein /usr/bin/lein
RUN chmod +x /usr/bin/lein

RUN mkdir -p -m 0600 ~/.ssh && ssh-keyscan github.com >> ~/.ssh/known_hosts
RUN mkdir -p /root/.config/truffle/
RUN npm install --global truffle@~5.3.0
RUN lein version
VOLUME [ "/root/.m2" ]
