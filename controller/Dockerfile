FROM openremote/controller:latest

WORKDIR /opt/OpenRemote-Controller/bin

# TODO The Web Console should be retrieved as a dependency or it should be in platform project
ADD webconsole.war /opt/OpenRemote-Controller/webapps/

RUN rm -r /opt/or-resources/*

ADD conf/.password /opt/or-resources/.password
ADD conf/.user /opt/or-resources/.user
ADD conf/controller.xml /opt/or-resources/controller.xml

ADD conf/env.drl /opt/or-resources/rules/env.drl
ENV RULES_DATA /opt/or-resources/data/

ADD conf/GVconfig.txt /opt/or-resources/data/GVconfig.txt

# Fix some permissions
USER root
RUN chown -R openremote.openremote /opt/or-resources
USER openremote

# Automatically configure Tomcat from env variables on startup
ADD conf/server_tpl.xml ../conf/
ADD run.sh ./
RUN chmod +x ./run.sh

# SSL configuration
ADD conf/keystore.jks ../
ADD conf/truststore.jks ../

ENTRYPOINT ["/opt/OpenRemote-Controller/bin/run.sh"]
