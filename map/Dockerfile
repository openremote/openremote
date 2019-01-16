# Adds varnish-cache to the container to improve raster tile serving
FROM klokantech/tileserver-gl
MAINTAINER support@openremote.io
RUN mkdir -p /server
WORKDIR /server
COPY config.json .
COPY fonts fonts


RUN apt-get -qq update \
&& apt-get upgrade -yqq \
&& DEBIAN_FRONTEND=noninteractive apt-get -y install \
    varnish \
&& apt-get clean

ADD default.vcl /etc/varnish/default.vcl
ADD start.sh /start.sh

ENTRYPOINT ["/bin/bash", "/start.sh"]

WORKDIR /data
CMD ["-c", "/server/config.json"]