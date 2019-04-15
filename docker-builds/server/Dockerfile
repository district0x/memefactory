FROM node:11.14.0-stretch
MAINTAINER "Filip Bielejec" <filip@district0x.io>

# Python dependencies
RUN apt-get update -y \
    && apt-get install --no-install-recommends -y \
    -q python-pip python-setuptools
RUN pip install cryptography base58

# ENV variables
ENV CONFIG /configs/meme.config.qa.edn

# get compiled JS
COPY server /memefactory/server
COPY node_modules /memefactory/node_modules
COPY resources /memefactory/resources

### --- DEBUG
# RUN ls memefactory/
# RUN ls memefactory/server/

WORKDIR memefactory

ENTRYPOINT ["node", "server/memefactory.js"]
CMD ["--max-old-space-size=2048"]
