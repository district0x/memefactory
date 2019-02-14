FROM district0x/base:latest
MAINTAINER "Filip Bielejec" <filip@district0x.io>

# Python dependencies
RUN apt-get -y --allow-releaseinfo-change update
RUN apt-get -y install libffi-dev libssl-dev
RUN apt-get -y install python-pip
RUN pip install cryptography --force-reinstall
RUN pip install base58

# fix for the checksum problem
# see https://stackoverflow.com/questions/47545940/npm-err-code-eintegrity-npm-5-3-0
RUN npm config set package-lock false

# ENV variables
ENV CONFIG /configs/meme.config.qa.edn

# get source code
COPY . /memefactory

# add smart_contracts.cljs with ropsten addresses
COPY docker-builds/shared/smart_contracts.cljs /src/memefactory/shared/smart_contracts.cljs

WORKDIR memefactory

# confirm contract addresses
#RUN cat /src/memefactory/shared/smart_contracts.cljs

# build server
RUN lein deps
RUN lein solc once
RUN lein cljsbuild once "server"

# expose to host
# VOLUME /configs
# VOLUME /logs
# EXPOSE 6300

CMD ["node", "server/memefactory.js"]
