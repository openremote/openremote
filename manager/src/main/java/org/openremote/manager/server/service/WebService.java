package org.openremote.manager.server.service;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Cookie;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.impl.CookieImpl;
import org.openremote.manager.server.util.JsonUtil;
import org.openremote.manager.shared.model.Credentials;
import org.openremote.manager.shared.model.LoginResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.vertx.core.http.HttpHeaders.LOCATION;
import static org.openremote.manager.server.Constants.*;
import static org.openremote.manager.server.util.UrlUtil.url;

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

        Router router = createRouter(vertx, staticHandler);

        server.requestHandler(router::accept).listen(completionHandler);
    }

    public void stop() {
        LOG.info("Stopping web service...");
        if (server != null) {
            server.close();
        }
    }

    protected Router createRouter(Vertx vertx, StaticHandler staticHandler) {

        Router router = Router.router(vertx);

        // Add cookie support
        router.route().handler(CookieHandler.create()::handle);

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
                    Credentials credentials = JsonUtil.JSON.readValue(bodyEvent.getBytes(), Credentials.class);
                    if ("admin".equals(credentials.getUsername()) && "password".equals(credentials.getPassword())) {
                        if (tokenCookie == null) {
                            tokenCookie = Cookie.cookie("access_token", "");
                            routingContext.addCookie(tokenCookie);
                        }
                        if (xsrfCookie == null) {
                            xsrfCookie = Cookie.cookie("XSRF-TOKEN","");
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
                } catch (IOException e) {
                    // Invalid credentials object supplied
                    loginResult = new LoginResult(400, null);
                }

                String loginResultStr = "";

                try {
                    loginResultStr = JsonUtil.JSON.writeValueAsString(loginResult);
                } catch (IOException e) {
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

        // The order of the following routes is important and tricky

        // Serve static resources with path /static/*
        router.route(STATIC_PATH + "/*").handler(context -> {
                HttpServerRequest request = context.request();
                HttpServerResponse response = context.response();

                // Special handling for compression of pbf static files (they are already compressed...)
                if (request.path().endsWith(".pbf")) {
                    response.putHeader(HttpHeaders.CONTENT_TYPE, "application/x-protobuf");
                    response.putHeader(HttpHeaders.CONTENT_ENCODING, "gzip");
                }

                staticHandler.handle(context);
            }
        );

        // Serve API resources with path /api/* and fail if there is no realm context parameter
        router.route(API_PATH + "/*").handler(
            context -> {
                String realm = context.get(CONTEXT_PARAM_REALM);
                if (realm == null) {
                    context.response().setStatusCode(400);
                    context.response().end("Missing realm as first path element");
                } else {
                    context.next();
                }
            });

        // Add sub-routers which must register under /api/* path
        addSubRouters(vertx, router, API_PATH);

        // If none of the /api/* sub-routers ended the request, end it here so we don't loop
        router.route(API_PATH + "/*").handler(
            context -> {
                if (!context.response().ended()) {
                    context.response().setStatusCode(404);
                    context.response().end("API not found");
                }
            });

        // Redirect root request to default /<realm> so browser has the realm in Window.Location for API calls
        router.route("/")
            .handler(context -> {
                context.response().putHeader(LOCATION, url(context, DEFAULT_REALM).toString());
                context.response().setStatusCode(302);
                context.response().end();
            });

        // Reroute /<realm> to /<realm>/
        router.routeWithRegex("\\/([a-z]+)")
            .handler(context -> {
                String realm = context.request().getParam("param0");
                context.reroute("/" + realm + "/");
            });

        // Serve static index.html for /<realm>/
        router.routeWithRegex("\\/([a-z]+)\\/")
            .handler(context -> context.reroute(STATIC_PATH + "/index.html"));

        // Reroute /<realm>/* to /<API PATH>/* and extract the realm as a context parameter
        router.routeWithRegex("\\/([a-z]+)\\/(.*)")
            .handler(context -> {
                String realm = context.request().getParam("param0");
                String remainingPath = "/" + context.request().getParam("param1");
                context.put(CONTEXT_PARAM_REALM, realm);
                context.reroute(API_PATH + remainingPath);
            });

        return router;
    }

    protected void addSubRouters(Vertx vertx, Router router, String apiPath) {
        router.mountSubRouter(apiPath + "/map", new MapRouter(vertx, mapService));
    }
}