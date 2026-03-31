cd "$(git rev-parse --show-toplevel)" # Ensure we can call the script from anywhere

# --- Default Environment Variables ---
export TZ=${TZ:-"Europe/Amsterdam"}
export OR_SSL_PORT=${OR_SSL_PORT:-"-1"}
export OR_HOSTNAME=${OR_HOSTNAME:-"localhost"}
export OR_EMAIL_PORT=${OR_EMAIL_PORT:-"587"}
export OR_EMAIL_TLS=${OR_EMAIL_TLS:-"true"}
export OR_EMAIL_PROTOCOL=${OR_EMAIL_PROTOCOL:-"smtp"}
export OR_FIREBASE_CONFIG_FILE=${OR_FIREBASE_CONFIG_FILE:-"deployment/manager/fcm.json"} # Removed leading slash for local relative path
export OR_DEV_MODE=${OR_DEV_MODE:-"true"}
export OR_SETUP_RUN_ON_RESTART=${OR_SETUP_RUN_ON_RESTART:-"true"}
export OR_WEBSERVER_LISTEN_HOST=${OR_WEBSERVER_LISTEN_HOST:-"0.0.0.0"}
export OR_DB_VENDOR=${OR_DB_VENDOR:-"postgres"}
export OR_DB_HOST=${OR_DB_HOST:-"localhost"} # Changed from 'postgresql' to 'localhost' for local running
export OR_DB_PORT=${OR_DB_PORT:-"5432"}
export OR_DB_NAME=${OR_DB_NAME:-"openremote"}
export OR_DB_SCHEMA=${OR_DB_SCHEMA:-"openremote"}
export OR_DB_USER=${OR_DB_USER:-"postgres"}
export OR_DB_POOL_MIN_SIZE=${OR_DB_POOL_MIN_SIZE:-"5"}
export OR_DB_POOL_MAX_SIZE=${OR_DB_POOL_MAX_SIZE:-"20"}
export OR_DB_CONNECTION_TIMEOUT_SECONDS=${OR_DB_CONNECTION_TIMEOUT_SECONDS:-"300"}
export OR_KEYCLOAK_HOST=${OR_KEYCLOAK_HOST:-"localhost"} # Changed from 'keycloak' to 'localhost'
export OR_KEYCLOAK_PORT=${OR_KEYCLOAK_PORT:-"8081"}
export OR_KEYCLOAK_GRANT_FILE=${OR_KEYCLOAK_GRANT_FILE:-"manager/keycloak-credentials.json"}
export OR_APP_DOCROOT=${OR_APP_DOCROOT:-"manager/build/install/manager/web"} # Adjusted for local build
export OR_CUSTOM_APP_DOCROOT=${OR_CUSTOM_APP_DOCROOT:-"deployment/manager/app"} # Removed leading slash
export OR_PROVISIONING_DOCROOT=${OR_PROVISIONING_DOCROOT:-"deployment/manager/provisioning"} # Removed leading slash
export OR_ROOT_REDIRECT_PATH=${OR_ROOT_REDIRECT_PATH:-"/manager"}
export OR_MAP_TILES_PATH=${OR_MAP_TILES_PATH:-"deployment.local/mapdata/mapdata.mbtiles"} # Removed leading slash
export OR_MAP_SETTINGS_PATH=${OR_MAP_SETTINGS_PATH:-"deployment/map/mapsettings.json"} # Removed leading slash
export OR_MAP_TILESERVER_PORT=${OR_MAP_TILESERVER_PORT:-"8082"}
export OR_MAP_TILESERVER_REQUEST_TIMEOUT=${OR_MAP_TILESERVER_REQUEST_TIMEOUT:-"10000"}
export OR_IDENTITY_PROVIDER=${OR_IDENTITY_PROVIDER:-"keycloak"}
export OR_IDENTITY_SESSION_MAX_MINUTES=${OR_IDENTITY_SESSION_MAX_MINUTES:-"1440"}
export OR_IDENTITY_SESSION_OFFLINE_TIMEOUT_MINUTES=${OR_IDENTITY_SESSION_OFFLINE_TIMEOUT_MINUTES:-"2628000"}
export OR_STORAGE_DIR=${OR_STORAGE_DIR:-"./tmp"} # Changed to relative local directory
export OR_GATEWAY_TUNNEL_LOCALHOST_REWRITE=${OR_GATEWAY_TUNNEL_LOCALHOST_REWRITE:-"127.0.0.1"} # Changed to localhost
export OR_JAVA_OPTS=${OR_JAVA_OPTS:-"-Xms500m -Xmx2g -XX:NativeMemoryTracking=summary -Xlog:all=warning:stdout:uptime,level,tags -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=./dump.hprof"}
export PROMETHEUS_DISABLE_CREATED_SERIES=${PROMETHEUS_DISABLE_CREATED_SERIES:-"true"}

# Variables without defaults
export JAVA_TOOL_OPTIONS=${JAVA_TOOL_OPTIONS:-""}
export OR_SETUP_TYPE=${OR_SETUP_TYPE:-""}
export OR_EMAIL_HOST=${OR_EMAIL_HOST:-""}
export OR_EMAIL_USER=${OR_EMAIL_USER:-""}
export OR_EMAIL_FROM=${OR_EMAIL_FROM:-""}
export OR_EMAIL_ADMIN=${OR_EMAIL_ADMIN:-""}
export OR_EMAIL_X_HEADERS=${OR_EMAIL_X_HEADERS:-""}
export OR_DB_FLYWAY_OUT_OF_ORDER=${OR_DB_FLYWAY_OUT_OF_ORDER:-""}
export OR_LOGGING_CONFIG_FILE=${OR_LOGGING_CONFIG_FILE:-"ui/test/logging.properties"}
export OR_MAP_TILESERVER_HOST=${OR_MAP_TILESERVER_HOST:-""}
export OR_ATTRIBUTE_EVENT_THREADS=${OR_ATTRIBUTE_EVENT_THREADS:-""}

# --- Local Paths ---
# Ensure you have run `./gradlew :manager:installDist` so these folders exist.
APP_LIB_DIR="manager/build/install/manager/lib/*"
EXTENSIONS_DIR="deployment/manager/extensions/*"

# --- Execution ---
echo "Starting OpenRemote Manager..."
echo "Classpath: ${APP_LIB_DIR}:${EXTENSIONS_DIR}"

exec java ${OR_JAVA_OPTS} -cp "${APP_LIB_DIR}:${EXTENSIONS_DIR}" org.openremote.manager.Main
