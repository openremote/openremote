package org.openremote.manager.server;

public interface Constants {

    String DEV_MODE = "DEV_MODE";
    boolean DEV_MODE_DEFAULT = true;

    String NETWORK_SECURE = "NETWORK_SECURE";
    boolean NETWORK_SECURE_DEFAULT = false;

    String NETWORK_HOST = "NETWORK_HOST";
    String NETWORK_HOST_DEFAULT = "localhost";

    String NETWORK_WEBSERVER_PORT = "NETWORK_WEBSERVER_PORT";
    int NETWORK_WEBSERVER_PORT_DEFAULT = 8080;

    String AUTH_PATH = "/auth";
    String API_PATH = "/api";
    String STATIC_PATH = "/static";

    String MANAGER_CLIENT_ID = "or-manager";
    String MASTER_REALM = "master";
}
