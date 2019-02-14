FROM district0x/base:latest
MAINTAINER "Filip Bielejec" <filip@district0x.io>

# ENV variables
ENV MEMEFACTORY_ENV=qa

# get source code
COPY . /memefactory

# add smart_contracts.cljs with ropsten addresses
COPY docker-builds/shared/smart_contracts.cljs /src/memefactory/shared/smart_contracts.cljs

WORKDIR memefactory

# confirm contract addresses
RUN cat /src/memefactory/shared/smart_contracts.cljs

# build ui
lein deps
lein garden once
lein solc once
lein cljsbuild once "ui"

# install nginx
RUN apt-get update -y \
    && apt-get install --no-install-recommends -y \
    -q nginx

# replace nginx config
ADD nginx.conf /etc/nginx/nginx.conf

# add digest auth file
ADD memefactory.passwd /etc/nginx/memefactory.passwd

# setup error page
RUN wget --no-check-certificate -O X0X.html https://raw.githubusercontent.com/district0x/X0X/master/X0X.html \
  && mv X0X.html /usr/share/nginx/html/X0X.html

# replace default server
ADD default /etc/nginx/sites-available/default

# nginx config
ADD memefactory.io /etc/nginx/sites-available/memefactory.io
RUN ln -s -f /etc/nginx/sites-available/memefactory.io /etc/nginx/sites-enabled/memefactory.io

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]




