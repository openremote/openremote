package org.openremote.manager.server;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideResponse;
import com.yahoo.elide.audit.Slf4jLogger;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.datastores.hibernate5.HibernateStore;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import org.hibernate.SessionFactory;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

import static org.openremote.manager.server.Constants.DEV_MODE;
import static org.openremote.manager.server.Constants.DEV_MODE_DEFAULT;

public class ServerVerticle extends AbstractVerticle {

    private static final Logger LOG = Logger.getLogger(ServerVerticle.class.getName());

    public static final String WEB_SERVER_PORT = "WEB_SERVER_PORT";
    public static final int WEB_SEVER_PORT_DEFAULT = 8080;

    public static final String WEB_SERVER_DOCROOT = "WEB_SERVER_DOCROOT";
    public static final String WEB_SERVER_DOCROOT_DEFAULT = "manager/src/main/webapp";

    protected boolean devMode;

    protected PersistenceService persistenceService;

    protected SampleData sampleData;

    protected Elide elide;

    @Override
    public void start(Future<Void> startFuture) {

        devMode = config().getBoolean(DEV_MODE, DEV_MODE_DEFAULT);

        vertx.executeBlocking(
            blocking -> {
                startPersistenceService();
                startWebServer(event -> {
                    if (event.succeeded()) {
                        blocking.complete();
                    } else {
                        blocking.fail(event.cause());
                    }
                });
            },
            result -> {
                if (result.succeeded()) {
                    startFuture.complete();
                } else {
                    startFuture.fail(result.cause());
                }
            }
        );
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        try {
            if (devMode && sampleData != null)
                sampleData.drop(persistenceService);
            persistenceService.stop();
            stopFuture.complete();
        } catch (Exception ex) {
            stopFuture.fail(ex);
        }
    }

    protected void startPersistenceService() {
        persistenceService = new PersistenceService();
        persistenceService.start(config());

        if (devMode) {
            sampleData = new SampleData();
            sampleData.create(persistenceService);
        }

        SessionFactory sessionFactory =
            persistenceService.getEntityManagerFactory().unwrap(SessionFactory.class);
        DataStore dataStore = new HibernateStore(sessionFactory);
        com.yahoo.elide.audit.Logger auditLogger = new Slf4jLogger();
        elide = new Elide(auditLogger, dataStore);
    }

    protected void startWebServer(Handler<AsyncResult<HttpServer>> completionHandler) {

        HttpServerOptions options = new HttpServerOptions();
        HttpServer server = vertx.createHttpServer(options);
        Router router = Router.router(vertx);

        router.route("/hello").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "text/plain");
            response.end("Hello from Server, the time is: " + System.currentTimeMillis());
        });

        router.route("/device").handler(routingContext -> {

            vertx.executeBlocking(
                event -> {
                    MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
                    // TODO: Copy request parameters into map
                    try {
                        ElideResponse response = elide.get("/device", params, "someUsername");
                        event.complete(response);
                    } catch (Exception ex) {
                        event.fail(ex);
                    }
                },
                result -> {
                    if (result.succeeded()) {
                        ElideResponse elideResponse = (ElideResponse) result.result();
                        HttpServerResponse response = routingContext.response();
                        response.putHeader("content-type", "application/json");
                        response.end(elideResponse.getBody());
                    } else {
                        HttpServerResponse response = routingContext.response();
                        response.setStatusCode(500).end(result.cause().toString());
                    }
                }
            );
        });

        Path docRoot = Paths.get(config().getString(WEB_SERVER_DOCROOT, WEB_SERVER_DOCROOT_DEFAULT));
        if (!Files.exists(docRoot)) {
            throw new IllegalStateException(
                "Static web document root doesn't exist: " + docRoot.toAbsolutePath()
            );
        }
        LOG.info("Configuring static document root path: " + docRoot.toAbsolutePath());
        router.route("/*").handler(
            StaticHandler.create(
                docRoot.toString()
            ).setCachingEnabled(false)
        );

        int webserverPort = config().getInteger(WEB_SERVER_PORT, WEB_SEVER_PORT_DEFAULT);
        LOG.info("Starting web server on port: " + webserverPort);
        server
            .requestHandler(router::accept)
            .listen(webserverPort, completionHandler);
    }
}