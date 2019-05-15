FROM prerendercloud/webserver:latest
MAINTAINER "Filip Bielejec" <filip@district0x.io>

# for debugging
RUN apt-get install --no-install-recommends -y -q nano

# get compiled JS
COPY resources/public /wwwroot

# setup redirects
ADD docker-builds/ui/prerender/_redirects /wwwroot/_redirects

EXPOSE 9000
