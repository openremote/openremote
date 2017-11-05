#!/bin/bash -e

BASE_PATH=/home/openremote/app

# If developer mode is enabled, all setup tasks will be executed on application startup.
# This will clean all data in the Manager database, and then import demo/test
# data. Some caching and other runtime performance-optimizations will also be disabled.
export DEV_MODE=true

# If you are not running in developer mode, and this is the first time you are starting
# the application, you can configure which setup tasks you want to execute. Careful,
# these tasks will clean and populate the Manager database with demo data!
export SETUP_WIPE_CLEAN_INSTALL=true
export SETUP_BASIC_IDENTITY_ADMIN_PASSWORD=${SETUP_BASIC_IDENTITY_ADMIN_PASSWORD:-"secret"}
export SETUP_IMPORT_DEMO_USERS=true
export SETUP_IMPORT_DEMO_ASSETS=true
export SETUP_IMPORT_DEMO_RULES=true

# Use basic identity service instead of Keycloak, set master admin password
export IDENTITY_PROVIDER=basic

# The public host name of this OpenRemote installation. This name must be the name you
# access the web services under.
export IDENTITY_NETWORK_HOST=${IDENTITY_NETWORK_HOST:-10.0.0.123}

# Set if SSL is enabled on the frontend reverse proxy and all internal proxies should assume https
export IDENTITY_NETWORK_SECURE=false

# The public port of this OpenRemote installation.
export IDENTITY_NETWORK_WEBSERVER_PORT=80

# Webserver listen port
export WEBSERVER_LISTEN_PORT=80

# The service-internal JDBC connection URL for the database service.
export DATABASE_CONNECTION_URL=jdbc:postgresql://localhost/openremote

# The database connection settings, passwords should be changed in production!
export DATABASE_USERNAME=openremote
export DATABASE_PASSWORD=${DATABASE_PASSWORD:-"CHANGE_ME_DATABASE_PASSWORD"}
export DATABASE_MIN_POOL_SIZE=5
export DATABASE_MAX_POOL_SIZE=20
export DATABASE_CONNECTION_TIMEOUT_SECONDS=300

# Paths
export MANAGER_DOCROOT=${BASE_PATH}/client/webapp
export CONSOLES_DOCROOT=${BASE_PATH}/server/deployment/manager/consoles
export UI_DOCROOT=${BASE_PATH}/server/deployment/manager/ui
export LOGGING_CONFIG_FILE=${BASE_PATH}/server/deployment/manager/logging.properties
export MAP_TILES_PATH=${BASE_PATH}/server/deployment/manager/map/mapdata.mbtiles
export MAP_SETTINGS_PATH=${BASE_PATH}/server/deployment/manager/map/mapsettings.json

java -cp "${BASE_PATH}/server/lib/*" org.openremote.manager.server.Main
