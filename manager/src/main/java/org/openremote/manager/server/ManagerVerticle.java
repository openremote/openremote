package org.openremote.manager.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import org.openremote.manager.shared.model.Asset;

import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.manager.server.util.JsonUtil.JSON;

public class ManagerVerticle extends AbstractVerticle {

    private static final Logger LOG = Logger.getLogger(ManagerVerticle.class.getName());

    @Override
    public void start(Future<Void> future) {

        HttpServerOptions options = new HttpServerOptions();
        HttpServer server = vertx.createHttpServer(options);

        Router router = Router.router(vertx);

        router.route("/hello").handler(routingContext -> {
            LOG.info("### HELLO SERVICE METHOD CALLED !!!");
            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "text/plain");
            response.end("Hello from Server, the time is: " + System.currentTimeMillis());
        });

        router.route("/asset").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "application/json");

            try {
                response.end(JSON.writeValueAsString(new Asset("Hello Asset")));
            } catch (JsonProcessingException e) {
                LOG.log(Level.SEVERE, "JSON Failed", e);
            }
        });

        router.route("/*").handler(StaticHandler.create("manager/src/main/webapp").setCachingEnabled(false));

        server
            .requestHandler(router::accept)
            .listen(8080, result -> {
                if (result.succeeded()) {
                    future.complete();
                } else {
                    future.fail(result.cause());
                }
            });

    }
}
