FROM ubuntu:latest
MAINTAINER "Filip Bielejec" <filip@district0x.io>

# install nginx
RUN apt-get update -y \
    && apt-get install --no-install-recommends -y \
    -q nginx

# replace nginx config
ADD nginx.conf /etc/nginx/nginx.conf

# replace default server
ADD default /etc/nginx/sites-available/default

# setup server config
ADD memefactory /etc/nginx/sites-available/memefactory
RUN ln -s -f /etc/nginx/sites-available/memefactory /etc/nginx/sites-enabled/memefactory

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
