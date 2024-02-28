# A Custom docker image that just includes jmeter - the standard taurus image is bloated with all executors - this also
# adds plugins used so custom plugins can be easily included
FROM eclipse-temurin:21-jre-alpine

ARG JMETER_VERSION=5.6.3

# Install python, pipx, bzt
# Download JMeter so we can load our custom plugins (taurus can only download plugins from the JMeter Plugins Manager)
# Download Plugins manager (for standard plugins resolution)
ENV PYTHONUNBUFFERED=1
RUN apk add --update --no-cache python3 pipx \
    && ln -sf python3 /usr/bin/python

RUN apk add --no-cache --virtual .build-deps py3-setuptools py3-wheel cython gcc musl-dev build-base linux-headers python3-dev libxml2-dev libxslt-dev zlib-dev net-tools \
    && pipx install bzt \
    && apk del .build-deps

ENV PATH="${PATH}:/root/.local/bin"

#RUN apk add --no-cache --virtual .build-deps gcc musl-dev \
#     && pipx install cython \
#     && apk del .build-deps


RUN cd /tmp \
    && wget -c https://dlcdn.apache.org/jmeter/binaries/apache-jmeter-${JMETER_VERSION}.tgz -O - | tar -xz \
    && mkdir -p /root/.bzt/jmeter-taurus \
    && mv apache-jmeter-${JMETER_VERSION} /root/.bzt/jmeter-taurus/${JMETER_VERSION} \
    && wget -P /root/.bzt/jmeter-taurus/${JMETER_VERSION}/lib/ext/ -c https://repo1.maven.org/maven2/kg/apc/jmeter-plugins-manager/1.10/jmeter-plugins-manager-1.10.jar \
    && wget -P /root/.bzt/jmeter-taurus/${JMETER_VERSION}/lib/ext/ -c https://github.com/richturner/mqtt-jmeter/releases/download/2.0.4-RT/plugin-xmeter-2.0.4-RT-jar-with-dependencies.jar \
    && mkdir /bzt-configs /tmp/artifacts

WORKDIR /bzt-configs/
ENTRYPOINT ["sh", "-c", "bzt -l /tmp/artifacts/bzt.log \"$@\"", "ignored"]
