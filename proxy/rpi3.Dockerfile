FROM openremote/rpi-raspbian:latest

ARG ACME_PLUGIN_VERSION=0.1.1

RUN [ "cross-build-start" ]

RUN apt-get update \
	&& apt-get install -y haproxy rsyslog inotify-tools \
    && rm -rf /var/lib/apt/lists/* \
    && rm /etc/haproxy/haproxy.cfg

RUN cd /etc/haproxy \
    && curl -sSL https://github.com/janeczku/haproxy-acme-validation-plugin/archive/${ACME_PLUGIN_VERSION}.tar.gz -o acme-plugin.tar.gz \
    && tar xvf acme-plugin.tar.gz --strip-components=1 --no-anchored acme-http01-webroot.lua \
    && rm *.tar.gz && cd

RUN [ "cross-build-end" ]

ADD rsyslog.conf /etc/rsyslog.conf
ADD haproxy-init.cfg /etc/haproxy/haproxy-init.cfg
ADD demo /etc/haproxy/demo

EXPOSE 80 443

COPY entrypoint.sh /
RUN chmod +x /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]