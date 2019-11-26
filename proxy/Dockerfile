FROM debian:stretch
MAINTAINER support@openremote.io

# Install utilities
RUN apt-get update && apt-get install -y --no-install-recommends \
        apt-transport-https \
        gnupg2 \
        software-properties-common \
        procps \
		ca-certificates \
		curl \
		wget \
	&& rm -rf /var/lib/apt/lists/*

RUN apt-get update \
      && apt-get install -y --no-install-recommends \
            certbot rsyslog cron inotify-tools make gcc g++ libreadline-dev libssl-dev libpcre3-dev libz-dev \
      && rm -rf /var/lib/apt/lists/*

ARG HA_PROXY_MINOR_VERSION=1.7
ARG HA_PROXY_VERSION=1.7.9
ARG LUA_VERSION=5.3.4
ARG ACME_PLUGIN_VERSION=0.1.1
ARG DOMAINNAME
ARG LOCAL_CERT_FILE
ENV DOMAINNAME=${DOMAINNAME:-localhost}
ENV LOCAL_CERT_FILE=$LOCAL_CERT_FILE
ENV TERM xterm
ENV PROXY_LOGLEVEL notice
ENV PROXY_BACKEND_HOST=${PROXY_BACKEND_HOST:-manager}
ENV PROXY_BACKEND_PORT=${PROXY_BACKEND_PORT:-8080}

RUN mkdir /tmp/lua && cd /tmp/lua \
    && curl -sSL https://www.lua.org/ftp/lua-${LUA_VERSION}.tar.gz -o lua.tar.gz \
    && tar xfv lua.tar.gz --strip-components=1 \
    && make linux && make install \
    && cd /tmp && rm -r lua

RUN mkdir /tmp/haproxy && cd /tmp/haproxy \
    && curl -sSL http://www.haproxy.org/download/${HA_PROXY_MINOR_VERSION}/src/haproxy-${HA_PROXY_VERSION}.tar.gz -o haproxy.tar.gz \
    && tar xfv haproxy.tar.gz --strip-components=1 \
    && make TARGET=linux2628 USE_PCRE=1 USE_OPENSSL=1 USE_ZLIB=1 USE_CRYPT_H=1 USE_LIBCRYPT=1 USE_LUA=1 && make install \
    && cd /tmp && rm -r haproxy

RUN mkdir /etc/haproxy && cd /etc/haproxy \
    && curl -sSL https://github.com/janeczku/haproxy-acme-validation-plugin/archive/${ACME_PLUGIN_VERSION}.tar.gz -o acme-plugin.tar.gz \
    && tar xvf acme-plugin.tar.gz --strip-components=1 --no-anchored acme-http01-webroot.lua \
    && rm *.tar.gz && cd

RUN apt-get purge --auto-remove -y make gcc g++ libreadline-dev libssl-dev libpcre3-dev libz-dev

RUN mkdir /opt/selfsigned

ADD rsyslog.conf /etc/rsyslog.conf
ADD haproxy-init.cfg /etc/haproxy/haproxy-init.cfg
ADD haproxy.cfg /etc/haproxy/haproxy.cfg
ADD selfsigned /opt/selfsigned

ADD cli.ini /root/.config/letsencrypt/

EXPOSE 80 443

HEALTHCHECK --interval=3s --timeout=3s --start-period=2s --retries=30 CMD curl --fail --silent http://localhost:80 || exit 1

COPY entrypoint.sh /
RUN chmod +x /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]
CMD ["run"]