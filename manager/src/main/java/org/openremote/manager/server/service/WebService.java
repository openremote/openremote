package org.openremote.manager.server.service;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebService {

    private static final Logger LOG = Logger.getLogger(WebService.class.getName());

    public static final String WEB_SERVER_PORT = "WEB_SERVER_PORT";
    public static final int WEB_SEVER_PORT_DEFAULT = 8080;
    public static final String WEB_SERVER_DOCROOT = "WEB_SERVER_DOCROOT";
    public static final String WEB_SERVER_DOCROOT_DEFAULT = "src/main/webapp";

    final protected ContextBrokerService contextBrokerService;
    final protected MapService mapService;
    final protected PersistenceService persistenceService;

    protected HttpServer server;
    protected Router router;

    public WebService(ContextBrokerService contextBrokerService,
                      MapService mapService,
                      PersistenceService persistenceService) {
        this.contextBrokerService = contextBrokerService;
        this.mapService = mapService;
        this.persistenceService = persistenceService;
    }

    public void start(Vertx vertx, JsonObject config, Handler<AsyncResult<HttpServer>> completionHandler) {
        int webserverPort = config.getInteger(WEB_SERVER_PORT, WEB_SEVER_PORT_DEFAULT);
        LOG.info("Starting web server on port: " + webserverPort);

        HttpServerOptions options = new HttpServerOptions();
        options.setPort(webserverPort);

        // TODO This will cause problems with the handling of PBF compression in the Mapbox client
        // options.setCompressionSupported(true);

        server = vertx.createHttpServer(options);
        router = Router.router(vertx);

        addRoutes();

        Path docRoot = Paths.get(config.getString(WEB_SERVER_DOCROOT, WEB_SERVER_DOCROOT_DEFAULT));
        if (!Files.exists(docRoot)) {
            throw new IllegalStateException(
                "Static web document root doesn't exist: " + docRoot.toAbsolutePath()
            );
        }
        LOG.info("Configuring static document root path: " + docRoot.toAbsolutePath());
        StaticHandler staticHandler = StaticHandler.create(
            docRoot.toString()
        ).setCachingEnabled(false);

        router.route("/*").handler(requestContext -> {
                HttpServerRequest request = requestContext.request();
                HttpServerResponse response = requestContext.response();

                // Special handling for compression of pbf static files (they are already compressed...)
                if (request.path().endsWith(".pbf")) {
                    response.putHeader(HttpHeaders.CONTENT_TYPE, "application/x-protobuf");
                    response.putHeader(HttpHeaders.CONTENT_ENCODING, "gzip");
                }

                staticHandler.handle(requestContext);
            }
        );

        server.requestHandler(router::accept).listen(completionHandler);
    }

    public void stop() {
        LOG.info("Stopping web service...");
        if (server != null) {
            server.close();
        }
    }

    protected void addRoutes() {

        router.route("/hello").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "text/plain");
            response.end("Hello from Server, the time is: " + System.currentTimeMillis());
        });

        router.route("/map").handler(routingContext -> {
            URI baseUri = URI.create(routingContext.request().absoluteURI());
            routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            routingContext.response().end(mapService.getMapSettings(
                baseUri.getScheme() + "://" + baseUri.getRawAuthority() + "/map/tile"
            ).toJson());
        });

        router.route("/map/tile/:zoom/:column/:row").blockingHandler(routingContext -> { // Blocking!
            try {
                int zoom = Integer.valueOf(routingContext.request().getParam("zoom"));
                int column = Integer.valueOf(routingContext.request().getParam("column"));
                int row = Integer.valueOf(routingContext.request().getParam("row"));

                // Flip y, oh why
                row = new Double(Math.pow(2, zoom) - 1 - row).intValue();

                byte[] tile = mapService.getMapTile(zoom, column, row);
                if (tile != null) {
                    routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/vnd.mapbox-vector-tile");
                    routingContext.response().putHeader(HttpHeaders.CONTENT_ENCODING, "gzip");
                    routingContext.response().end(Buffer.buffer(tile));
                } else {
                    LOG.fine("Map tile not found: " + routingContext.request().path());
                    routingContext.response().setStatusCode(404);
                }
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "Error getting map tile: " + routingContext.request().uri(), ex);
                routingContext.response().setStatusCode(500);
            } finally {
                routingContext.next();
            }
        }, false);
    }
}