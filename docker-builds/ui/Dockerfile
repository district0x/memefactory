FROM prerendercloud/webserver:latest
MAINTAINER "Filip Bielejec" <filip@district0x.io>

# get compiled JS
COPY resources/public /wwwroot

# setup redirects
ADD _redirects /wwwroot/_redirects

EXPOSE 9000
