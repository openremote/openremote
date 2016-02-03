package org.openremote.manager.server;

import io.vertx.core.Vertx;

import java.util.logging.Logger;

public class Server {

    private static final Logger LOG = Logger.getLogger(Server.class.getName());

    public static void main(String[] args) {
        Vertx vertx =  Vertx.vertx();

        vertx.deployVerticle(
            ManagerVerticle.class.getName(),
            event -> {
                LOG.info("Server startup complete.");
            }
        );
    }
}
