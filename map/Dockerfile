# This just adds the ability to run behind reverse proxy with a non empty path (i.e. /map) to tileserver-gl; they seem
# reluctant to add this functionality despite users requesting it so this just pulls down PR and uses that instead
# see https://github.com/klokantech/tileserver-gl/pull/230
FROM klokantech/tileserver-gl
MAINTAINER support@openremote.io
RUN mkdir -p /server
WORKDIR /server
COPY config.json .
COPY fonts fonts
WORKDIR /data
CMD ["-c", "/server/config.json"]