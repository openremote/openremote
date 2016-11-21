FROM tomcat:7.0.73-jre8

# Remove default webapps
RUN rm -rf /usr/local/tomcat/webapps/*

# Create certificate
RUN keytool -genkey -noprompt -genkey -keyalg RSA -alias selfsigned -keystore keystore.ks -storepass storepass -validity 360 -keysize 2048 -dname "CN=localhost, OU=localhost, O=localhost, L=, S=, C=" -keypass 'secret'

ADD conf/server_tpl.xml conf/
ADD lib/* lib/
ADD run.sh ./

# Reduce startup time
ENV JAVA_OPTS -Djava.security.egd=file:/dev/urandom

EXPOSE 8443

CMD /usr/local/tomcat/run.sh