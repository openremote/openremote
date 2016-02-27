package org.openremote.manager.server.web;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.EncodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Cookie;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;
import org.openremote.manager.server.identity.IdentityRouter;
import org.openremote.manager.server.identity.IdentityService;
import org.openremote.manager.server.map.MapRouter;
import org.openremote.manager.server.map.MapService;
import org.openremote.manager.server.contextbroker.ContextBrokerService;
import org.openremote.manager.server.persistence.PersistenceService;
import org.openremote.manager.shared.model.Credentials;
import org.openremote.manager.shared.model.LoginResult;
import rx.exceptions.Exceptions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.hubrick.vertx.rest.MediaType.TEXT_PLAIN_VALUE;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static io.vertx.core.http.HttpHeaders.LOCATION;
import static org.openremote.manager.server.Constants.*;
import static org.openremote.manager.server.util.UrlUtil.url;

public class WebService {

    private static final Logger LOG = Logger.getLogger(WebService.class.getName());

    public static final String WEB_SERVER_DOCROOT = "WEB_SERVER_DOCROOT";
    public static final String WEB_SERVER_DOCROOT_DEFAULT = "src/main/webapp";

    final protected IdentityService identityService;
    final protected ContextBrokerService contextBrokerService;
    final protected MapService mapService;
    final protected PersistenceService persistenceService;

    protected HttpServer server;

    public WebService(IdentityService identityService,
                      ContextBrokerService contextBrokerService,
                      MapService mapService,
                      PersistenceService persistenceService) {
        this.identityService = identityService;
        this.contextBrokerService = contextBrokerService;
        this.mapService = mapService;
        this.persistenceService = persistenceService;
    }

    public void start(Vertx vertx, JsonObject config, Handler<AsyncResult<HttpServer>> completionHandler) {
        int webserverPort = config.getInteger(NETWORK_WEBSERVER_PORT, NETWORK_WEBSERVER_PORT_DEFAULT);
        LOG.info("Starting web server on port: " + webserverPort);

        HttpServerOptions options = new HttpServerOptions();
        options.setPort(webserverPort);

        // TODO This will cause problems with the handling of PBF compression in the Mapbox client
        // options.setCompressionSupported(true);

        server = vertx.createHttpServer(options);

        Path docRoot = Paths.get(config.getString(WEB_SERVER_DOCROOT, WEB_SERVER_DOCROOT_DEFAULT));
        if (!Files.exists(docRoot)) {
            throw new IllegalStateException(
                "Static web document root doesn't exist: " + docRoot.toAbsolutePath()
            );
        }
        LOG.info("Configuring static document root path: " + docRoot.toAbsolutePath());

        Router router = createRouter(vertx, docRoot);

        server.requestHandler(router::accept).listen(completionHandler);
    }

    public void stop() {
        LOG.info("Stopping web service...");
        if (server != null) {
            server.close();
        }
    }

    protected Router createRouter(Vertx vertx, Path docRoot) {

        Router router = Router.router(vertx);

        StaticHandler staticHandler = StaticHandler.create(
            docRoot.toString()
        ).setCachingEnabled(false); // TODO Should cache in production

        // The order of the following routes is important and tricky

        // Handle failures
        router.route().failureHandler(createFailureHandler(vertx));

        // Add cookie support
        router.route().handler(createCookieHandler(vertx));

        // Serve static resources with path /static/*
        router.route(STATIC_PATH + "/*").handler(
            rc -> {
                HttpServerRequest request = rc.request();
                HttpServerResponse response = rc.response();

                // Special handling for compression of pbf static files (they are already compressed...)
                if (request.path().endsWith(".pbf")) {
                    response.putHeader(CONTENT_TYPE, "application/x-protobuf");
                    response.putHeader(HttpHeaders.CONTENT_ENCODING, "gzip");
                }

                staticHandler.handle(rc);
            }
        );

        // Serve API resources with path /api/* and fail if there is no realm context parameter
        router.route(API_PATH + "/*").handler(
            rc -> {
                String realm = rc.get(HttpRouter.CONTEXT_PARAM_REALM);
                if (realm == null) {
                    rc.fail(new ResponseException(400, "Missing realm"));
                } else {
                    rc.next();
                }
            });

        // Add sub-routers which must register under /api/* path
        addSubRouters(vertx, router, API_PATH);

        // If none of the /api/* sub-routers ended the request, end it here so we don't loop
        router.route(API_PATH + "/*").handler(
            rc -> {
                if (!rc.response().ended()) {
                    rc.fail(new ResponseException(404, "API not available"));
                }
            });

        // Redirect root request to default /<realm> so browser has the realm in Window.Location for API calls
        router.route("/")
            .handler(rc -> {
                rc.response().putHeader(LOCATION, url(rc, MASTER_REALM).toString());
                rc.response().setStatusCode(302);
                rc.response().end();
            });

        // Reroute /<realm> to /<realm>/
        router.routeWithRegex("\\/([a-z]+)")
            .handler(rc -> {
                String realm = rc.request().getParam("param0");
                rc.reroute("/" + realm + "/");
            });

        // Serve static index.html for /<realm>/
        router.routeWithRegex("\\/([a-z]+)\\/")
            .handler(rc -> rc.reroute(STATIC_PATH + "/index.html"));

        // Reroute /<realm>/* to /<API PATH>/* and extract the realm as a context parameter
        router.routeWithRegex("\\/([a-z]+)\\/(.*)")
            .handler(rc -> {
                String realm = rc.request().getParam("param0");
                String remainingPath = "/" + rc.request().getParam("param1");
                rc.put(HttpRouter.CONTEXT_PARAM_REALM, realm);
                rc.reroute(API_PATH + remainingPath);
            });

        return router;
    }

    protected Handler<RoutingContext> createFailureHandler(Vertx vertx) {
        return rc -> {

            Throwable failure = rc.failure();
            int responseStatusCode = rc.statusCode() > 0 ? rc.statusCode() : 500;
            String responseText = null;

            if (failure != null) {
                if (failure instanceof ResponseException) {
                    ResponseException exception = (ResponseException) failure;
                    responseStatusCode = exception.getStatusCode();
                    responseText  = exception.getMessage();
                    LOG.log(
                        Level.WARNING,
                        "Response exception handling " + rc.request().method() + " " + rc.request().absoluteURI() + ": " +
                            responseStatusCode + " " + responseText,
                        failure
                    );
                } else {
                    LOG.log(
                        Level.SEVERE,
                        "Exception handling " + rc.request().method() + " " + rc.request().absoluteURI(),
                        failure
                    );
                }
            }
            if (!rc.response().ended()) {
                if (responseText == null) {
                    switch (responseStatusCode) {
                        case 500:
                            responseText = "Internal Server Error - Please contact your system administrator";
                            break;
                        case 401:
                            responseText = "Unauthorized";
                            break;
                        case 403:
                            responseText = "Forbidden";
                            break;
                        case 404:
                            responseText = "Not Found";
                            break;
                        default:
                            responseText = "";
                            break;
                    }
                }

                rc.response().setStatusCode(responseStatusCode);
                rc.response().headers().add(CONTENT_TYPE, TEXT_PLAIN_VALUE);
                rc.response().end(responseText);
            }
       };
    }

    protected CookieHandler createCookieHandler(Vertx vertx) {
        return CookieHandler.create();
    }

    protected void addSubRouters(Vertx vertx, Router router, String apiPath) {

        // Dummy login endpoint
        router.route("/login").handler(routingContext -> {
            HttpServerRequest request = routingContext.request();
            HttpServerResponse response = routingContext.response();

            request.bodyHandler(bodyEvent -> {
                Cookie tokenCookie = routingContext.getCookie("access_token");
                Cookie xsrfCookie = routingContext.getCookie("XSRF-TOKEN");
                if (tokenCookie != null) {
                    // Invalidate existing cookie
                    tokenCookie.setMaxAge(0);
                }
                if (xsrfCookie != null) {
                    xsrfCookie.setMaxAge(0);
                }

                LoginResult loginResult;
                try {
                    Credentials credentials = Json.decodeValue(bodyEvent.toString("utf-8"), Credentials.class);
                    if ("admin".equals(credentials.getUsername()) && "admin".equals(credentials.getPassword())) {
                        if (tokenCookie == null) {
                            tokenCookie = Cookie.cookie("access_token", "");
                            routingContext.addCookie(tokenCookie);
                        }
                        if (xsrfCookie == null) {
                            xsrfCookie = Cookie.cookie("XSRF-TOKEN", "");
                            routingContext.addCookie(xsrfCookie);
                        }
                        String xsrfValue = "d9b9714c-7ac0-42e0-8696-2dae95dbc33e";
                        String tokenValue = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gU25vdyIsInhzcmZUb2tlbiI6ImQ5Yjk3MTRjLTdhYzAtNDJlMC04Njk2LTJkYWU5NWRiYzMzZSIsImFkbWluIjp0cnVlLCJleHAiOjI1NTYyOTd9.b1vrR5nCcd5BpPanyElsTJdgsSR4m4odX7UNpFlALHY";
                        tokenCookie.setValue(tokenValue);
                        tokenCookie.setMaxAge(Long.MIN_VALUE);
                        xsrfCookie.setValue(xsrfValue);
                        xsrfCookie.setMaxAge(Long.MIN_VALUE);
                        loginResult = new LoginResult(200, tokenValue);
                    } else {
                        loginResult = new LoginResult(401, null);
                    }
                } catch (DecodeException e) {
                    // Invalid credentials object supplied
                    loginResult = new LoginResult(400, null);
                }

                String loginResultStr = "";

                try {
                    loginResultStr = Json.encode(loginResult);
                } catch (EncodeException e) {
                    response.setStatusCode(500);
                    LOG.log(Level.SEVERE, e.getMessage());
                } finally {
                    response.putHeader("content-type", "application/json");
                    response.putHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                    response.putHeader("Expires", "0");
                    response.putHeader("Pragma", "no-cache");
                    response.end(loginResultStr);
                }
            });
        });

        router.mountSubRouter(apiPath + "/identity", new IdentityRouter(vertx, identityService));

        router.mountSubRouter(apiPath + "/map", new MapRouter(vertx, identityService, mapService));
    }
}