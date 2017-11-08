FROM jboss/keycloak:2.5.5.Final

ADD changeDatabase.xsl /opt/jboss/keycloak/
RUN java -jar /usr/share/java/saxon.jar -s:/opt/jboss/keycloak/standalone/configuration/standalone.xml -xsl:/opt/jboss/keycloak/changeDatabase.xsl -o:/opt/jboss/keycloak/standalone/configuration/standalone.xml; java -jar /usr/share/java/saxon.jar -s:/opt/jboss/keycloak/standalone/configuration/standalone-ha.xml -xsl:/opt/jboss/keycloak/changeDatabase.xsl -o:/opt/jboss/keycloak/standalone/configuration/standalone-ha.xml; rm /opt/jboss/keycloak/changeDatabase.xsl
RUN mkdir -p /opt/jboss/keycloak/modules/system/layers/base/org/postgresql/jdbc/main
RUN curl -o /opt/jboss/keycloak/modules/system/layers/base/org/postgresql/jdbc/main/postgresql-jdbc.jar http://repo1.maven.org/maven2/org/postgresql/postgresql/9.4.1209/postgresql-9.4.1209.jar 2>/dev/null
ADD module.xml /opt/jboss/keycloak/modules/system/layers/base/org/postgresql/jdbc/main/

ADD setProxyForwarding.xsl /opt/jboss/keycloak/
RUN java -jar /usr/share/java/saxon.jar -s:/opt/jboss/keycloak/standalone/configuration/standalone.xml -xsl:/opt/jboss/keycloak/setProxyForwarding.xsl -o:/opt/jboss/keycloak/standalone/configuration/standalone.xml

ADD devModeConfig.xsl /opt/jboss/keycloak/
RUN java -jar /usr/share/java/saxon.jar -s:/opt/jboss/keycloak/standalone/configuration/standalone.xml -xsl:/opt/jboss/keycloak/devModeConfig.xsl -o:/opt/jboss/keycloak/standalone/configuration/standalone.xml

# This produces a lot of log output, only enable when you must log all HTTP traffic into Keycloak
# ADD enableRequestDumping.xsl /opt/jboss/keycloak/
# RUN java -jar /usr/share/java/saxon.jar -s:/opt/jboss/keycloak/standalone/configuration/standalone.xml -xsl:/opt/jboss/keycloak/enableRequestDumping.xsl -o:/opt/jboss/keycloak/standalone/configuration/standalone.xml

RUN rm /opt/jboss/keycloak/*.xsl

HEALTHCHECK --interval=3s --timeout=3s --start-period=2s --retries=30 CMD curl --fail http://localhost:8080/auth || exit 1

ENTRYPOINT ["/opt/jboss/docker-entrypoint.sh"]
CMD ["-b", "0.0.0.0"]