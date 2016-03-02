package org.openremote.manager.server;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import org.openremote.manager.server.util.JsonUtil;

import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.manager.server.util.EnvironmentUtil.getEnvironment;

public class Server {

    private static final Logger LOG = Logger.getLogger(Server.class.getName());

    static {
        // One-time static configuration goes here
        JsonUtil.configure(Json.mapper);
    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        LOG.info("Starting server...");

        DeploymentOptions options = new DeploymentOptions()
            .setConfig(getEnvironment());

        vertx.deployVerticle(
            ServerVerticle.class.getName(),
            options,
            result -> {
                if (result.succeeded()) {
                    LOG.info("Server startup complete");
                } else {
                    LOG.log(Level.SEVERE, "Server startup failed", result.cause());
                    System.exit(1);
                }
            }
        );
    }
}
