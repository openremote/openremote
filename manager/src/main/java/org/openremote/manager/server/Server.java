package org.openremote.manager.server;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;

import java.util.logging.Logger;

import static org.openremote.manager.server.util.EnvironmentUtil.getEnvironment;

public class Server {

    private static final Logger LOG = Logger.getLogger(Server.class.getName());

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        LOG.info("Starting server...");

        DeploymentOptions options = new DeploymentOptions()
            .setConfig(getEnvironment());

        vertx.deployVerticle(
            ServerVerticle.class.getName(),
            options,
            event -> {
                LOG.info("Server startup complete");
            }
        );
    }
}
