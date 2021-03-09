#!/bin/bash -e

###########################
# Build/download Keycloak #
###########################

if [ "$GIT_REPO" != "" ]; then
    if [ "$GIT_BRANCH" == "" ]; then
        GIT_BRANCH="master"
    fi

    # Install Git
    microdnf install -y git

    # Install Maven
    cd /opt/jboss 
    curl -s https://apache.uib.no/maven/maven-3/3.5.4/binaries/apache-maven-3.5.4-bin.tar.gz | tar xz
    mv apache-maven-3.5.4 /opt/jboss/maven
    export M2_HOME=/opt/jboss/maven

    # Clone repository
    git clone --depth 1 https://github.com/$GIT_REPO.git -b $GIT_BRANCH /opt/jboss/keycloak-source

    # Build
    cd /opt/jboss/keycloak-source

    MASTER_HEAD=`git log -n1 --format="%H"`
    echo "Keycloak from [build]: $GIT_REPO/$GIT_BRANCH/commit/$MASTER_HEAD"

    $M2_HOME/bin/mvn -Pdistribution -pl distribution/server-dist -am -Dmaven.test.skip clean install
    
    cd /opt/jboss

    tar xfz /opt/jboss/keycloak-source/distribution/server-dist/target/keycloak-*.tar.gz

    # Remove temporary files
    rm -rf /opt/jboss/maven
    rm -rf /opt/jboss/keycloak-source
    rm -rf $HOME/.m2/repository
    
    mv /opt/jboss/keycloak-* /opt/jboss/keycloak
else
    echo "Keycloak from [download]: $KEYCLOAK_DIST"

    cd /opt/jboss/
    curl -L $KEYCLOAK_DIST | tar zx
    mv /opt/jboss/keycloak-* /opt/jboss/keycloak
fi

#####################
# Create DB modules #
#####################

mkdir -p /opt/jboss/keycloak/modules/system/layers/base/com/mysql/jdbc/main
cd /opt/jboss/keycloak/modules/system/layers/base/com/mysql/jdbc/main
curl -O https://repo1.maven.org/maven2/mysql/mysql-connector-java/$JDBC_MYSQL_VERSION/mysql-connector-java-$JDBC_MYSQL_VERSION.jar
cp /opt/jboss/tools/databases/mysql/module.xml .
sed "s/JDBC_MYSQL_VERSION/$JDBC_MYSQL_VERSION/" /opt/jboss/tools/databases/mysql/module.xml > module.xml

mkdir -p /opt/jboss/keycloak/modules/system/layers/base/org/postgresql/jdbc/main
cd /opt/jboss/keycloak/modules/system/layers/base/org/postgresql/jdbc/main
curl -L https://repo1.maven.org/maven2/org/postgresql/postgresql/$JDBC_POSTGRES_VERSION/postgresql-$JDBC_POSTGRES_VERSION.jar > postgres-jdbc.jar
cp /opt/jboss/tools/databases/postgres/module.xml .

mkdir -p /opt/jboss/keycloak/modules/system/layers/base/org/mariadb/jdbc/main
cd /opt/jboss/keycloak/modules/system/layers/base/org/mariadb/jdbc/main
curl -L https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/$JDBC_MARIADB_VERSION/mariadb-java-client-$JDBC_MARIADB_VERSION.jar > mariadb-jdbc.jar
cp /opt/jboss/tools/databases/mariadb/module.xml .

mkdir -p /opt/jboss/keycloak/modules/system/layers/base/com/oracle/jdbc/main
cd /opt/jboss/keycloak/modules/system/layers/base/com/oracle/jdbc/main
cp /opt/jboss/tools/databases/oracle/module.xml .

mkdir -p /opt/jboss/keycloak/modules/system/layers/keycloak/com/microsoft/sqlserver/jdbc/main
cd /opt/jboss/keycloak/modules/system/layers/keycloak/com/microsoft/sqlserver/jdbc/main
curl -L https://repo1.maven.org/maven2/com/microsoft/sqlserver/mssql-jdbc/$JDBC_MSSQL_VERSION/mssql-jdbc-$JDBC_MSSQL_VERSION.jar > mssql-jdbc.jar
cp /opt/jboss/tools/databases/mssql/module.xml .

######################
# Configure Keycloak #
######################

cd /opt/jboss/keycloak

bin/jboss-cli.sh --file=/opt/jboss/tools/cli/standalone-configuration.cli
rm -rf /opt/jboss/keycloak/standalone/configuration/standalone_xml_history

bin/jboss-cli.sh --file=/opt/jboss/tools/cli/standalone-ha-configuration.cli
rm -rf /opt/jboss/keycloak/standalone/configuration/standalone_xml_history

###########
# Garbage #
###########

rm -rf /opt/jboss/keycloak/standalone/tmp/auth
rm -rf /opt/jboss/keycloak/domain/tmp/auth

###################
# Set permissions #
###################

echo "jboss:x:0:root" >> /etc/group
echo "jboss:x:1000:0:JBoss user:/opt/jboss:/sbin/nologin" >> /etc/passwd
chown -R jboss:root /opt/jboss
chmod -R g+rwX /opt/jboss
