/*
 * Copyright 2016, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.manager.web;

import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.CanonicalPathHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.RedirectHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.util.HttpString;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.openremote.container.Container;
import org.openremote.container.security.IdentityService;
import org.openremote.container.web.WebService;
import org.openremote.container.web.jsapi.JSAPIServlet;
import org.openremote.manager.asset.AssetStorageService;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.undertow.util.RedirectBuilder.redirect;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.UriBuilder.fromUri;
import static org.openremote.container.util.MapAccess.getBoolean;
import static org.openremote.container.util.MapAccess.getString;
import static org.openremote.model.Constants.REQUEST_HEADER_REALM;

public class ManagerWebService extends WebService {

    public static final int PRIORITY = LOW_PRIORITY + 100;
    public static final String APP_DOCROOT = "APP_DOCROOT";
    public static final String APP_DOCROOT_DEFAULT = "deployment/build/manager/app";
    public static final String SHARED_DOCROOT = "SHARED_DOCROOT";
    public static final String SHARED_DOCROOT_DEFAULT = "deployment/manager/shared";
    public static final String CONSOLE_USE_STATIC_BOWER_COMPONENTS = "CONSOLE_USE_STATIC_BOWER_COMPONENTS";
    public static final boolean CONSOLE_USE_STATIC_BOWER_COMPONENTS_DEFAULT = true;
    public static final String APP_DEFAULT = "APP_DEFAULT";
    public static final String APP_DEFAULT_DEFAULT = "main";
    public static final String API_PATH = "/api";
    public static final String JSAPI_PATH = "/jsapi";
    public static final String STATIC_PATH = "/static";
    public static final String SHARED_PATH = "/shared";
    public static final String CONSOLE_PATH = "/console";
    public static final String APP_PATH = "/app";
    private static final Logger LOG = Logger.getLogger(ManagerWebService.class.getName());
    protected static final Pattern PATTERN_REALM_SUB = Pattern.compile("/([a-zA-Z0-9\\-_]+)/(.*)");

    protected Path appDocRoot;
    protected Path sharedDocRoot;
    protected Collection<Class<?>> apiClasses = new HashSet<>();
    protected Collection<Object> apiSingletons = new HashSet<>();

    /**
     * Start web service after other services.
     */
    @Override
    public int getPriority() {
        return PRIORITY;
    }
    @Override
    public void init(Container container) throws Exception {
        super.init(container);

        IdentityService identityService = container.getService(IdentityService.class);
        String defaultApp = getString(container.getConfig(), APP_DEFAULT, APP_DEFAULT_DEFAULT);

        ResteasyDeployment resteasyDeployment = createResteasyDeployment(container, getApiClasses(), getApiSingletons(), true);

        // Serve REST API
        HttpHandler apiHandler = createApiHandler(identityService, resteasyDeployment);
        // Serve JavaScript API
        // TODO: Remove this once all apps updated to new structure
        HttpHandler jsApiHandler = createJsApiHandler(identityService, resteasyDeployment);

        if (apiHandler != null) {

            // Authenticating requests requires a realm, either we receive this in a header or
            // we extract it (e.g. from request path segment) and set it as a header before
            // processing the request
            HttpHandler baseApiHandler = apiHandler;

            apiHandler = exchange -> {

                String path = exchange.getRelativePath().substring(API_PATH.length());
                Matcher realmSubMatcher = PATTERN_REALM_SUB.matcher(path);

                if (!realmSubMatcher.matches()) {
                    exchange.setStatusCode(NOT_FOUND.getStatusCode());
                    throw new WebApplicationException(NOT_FOUND);
                }

                // Extract realm from path and push it into REQUEST_HEADER_REALM header
                String realm = realmSubMatcher.group(1);

                // Move the realm from path segment to header
                exchange.getRequestHeaders().put(HttpString.tryFromString(REQUEST_HEADER_REALM), realm);

                URI url = fromUri(exchange.getRequestURL())
                        .replacePath(realmSubMatcher.group(2))
                        .build();
                exchange.setRequestURI(url.toString(), true);
                exchange.setRequestPath(url.getPath());
                exchange.setRelativePath(url.getPath());

                baseApiHandler.handleRequest(exchange);
            };
        }

        // Serve deployment files unsecured (explicitly map deployment folders to request paths)
        appDocRoot = Paths.get(getString(container.getConfig(), APP_DOCROOT, APP_DOCROOT_DEFAULT));
        sharedDocRoot = Paths.get(getString(container.getConfig(), SHARED_DOCROOT, SHARED_DOCROOT_DEFAULT));
        HttpHandler sharedFileHandler = createFileHandler(devMode, identityService, sharedDocRoot, null);
        HttpHandler appBaseFileHandler = Files.isDirectory(appDocRoot) ? createFileHandler(devMode, identityService, appDocRoot, null) : null;

        // Default app file handler to use index.html
        HttpHandler appFileHandler = exchange -> {
            if (exchange.getRelativePath().isEmpty() || "/".equals(exchange.getRelativePath())) {
                exchange.setRelativePath("/index.html");
            }
            if (appBaseFileHandler != null) {
                appBaseFileHandler.handleRequest(exchange);
            }
        };

        // TODO: Remove this once GWT client is replaced
        // Add a file handler for GWT source files in dev mode
        Path gwtSourceDir = Paths.get("client/src/main/webapp");

        if (Files.exists(gwtSourceDir) && container.isDevMode()) {
            final HttpHandler baseGwtFileHandler = createFileHandler(devMode, identityService, gwtSourceDir, null);
            final HttpHandler gwtFileHandler = exchange -> {
                if (exchange.getRelativePath().isEmpty() || "/".equals(exchange.getRelativePath())) {
                    exchange.setRelativePath("/index.html");
                }
                baseGwtFileHandler.handleRequest(exchange);
            };
            HttpHandler standardHandler = appFileHandler;

            appFileHandler = exchange -> {
                String path = exchange.getRelativePath();
                if (path.startsWith("/manager")) {
                    exchange.setRelativePath(path.substring(8));
                    gwtFileHandler.handleRequest(exchange);
                } else if (path.startsWith("/app/manager")) {
                    exchange.setRelativePath(path.substring(12));
                    gwtFileHandler.handleRequest(exchange);
                } else {
                    standardHandler.handleRequest(exchange);
                }
            };
        }
        HttpHandler finalAppFileHandler = appFileHandler;

        // Serve deployment files
        PathHandler deploymentHandler = new PathHandler(appFileHandler)
                // TODO: Update this static file http handler to use shared folder in deployment
                .addPrefixPath(STATIC_PATH, exchange -> {
                    // TODO: Remove this horrible hack for crappy polymer 2.x NPM package relative import issue
                    if (exchange.getRelativePath().startsWith("/node_modules/@polymer/shadycss")) {
                        exchange.setRelativePath("/node_modules/@webcomponents/shadycss" + exchange.getRequestPath().substring(38));
                        exchange.setRequestPath(STATIC_PATH + exchange.getRelativePath());
                    }

                    exchange.setRelativePath("/manager" + exchange.getRelativePath());
                    finalAppFileHandler.handleRequest(exchange);
                })
                .addPrefixPath(APP_PATH, appFileHandler)
                // TODO: Remove this path prefix at some point
                .addPrefixPath(CONSOLE_PATH, appFileHandler)
                .addPrefixPath(SHARED_PATH, sharedFileHandler);

        // TODO: Remove this once all apps updated to new structure
        final boolean useStaticBowerComponents =
                getBoolean(container.getConfig(), CONSOLE_USE_STATIC_BOWER_COMPONENTS, CONSOLE_USE_STATIC_BOWER_COMPONENTS_DEFAULT);
        if (useStaticBowerComponents) {
            deploymentHandler.addPrefixPath("/bower_components", exchange -> {
                exchange.setRequestPath("/manager/bower_components" + exchange.getRequestPath());
                finalAppFileHandler.handleRequest(exchange);
            });
        }


        // Add all route handlers required by the manager in priority order

        // Redirect / to default app
        getRequestHandlers().add(
                new RequestHandler(
                        "Default app redirect",
                        exchange -> exchange.getRequestPath().equals("/"),
                        exchange -> {
                            LOG.fine("Handling root request, redirecting client to default app");
                            new RedirectHandler(redirect(exchange, "/" + defaultApp)).handleRequest(exchange);
                        }));

        if (jsApiHandler != null) {
            getRequestHandlers().add(pathStartsWithHandler("REST JS API Handler", JSAPI_PATH, jsApiHandler));
        }

        if (apiHandler != null) {
            getRequestHandlers().add(pathStartsWithHandler("REST API Handler", API_PATH, apiHandler));
        }

        // This will try and handle any request that makes it to this handler
        getRequestHandlers().add(
                new RequestHandler(
                        "Deployment files",
                        exchange -> true,
                        deploymentHandler
                )
        );
    }

    /**
     * Add resource/provider/etc. classes to enable REST API
     */
    public Collection<Class<?>> getApiClasses() {
        return apiClasses;
    }

    /**
     * Add resource/provider/etc. singletons to enable REST API.
     */
    public Collection<Object> getApiSingletons() {
        return apiSingletons;
    }

    public Path getAppDocRoot() {
        return appDocRoot;
    }

    public String getConsoleUrl(UriBuilder baseUri, String realm) {
        return baseUri.path(CONSOLE_PATH).path(realm).build().toString();
    }

    protected HttpHandler createApiHandler(IdentityService identityService, ResteasyDeployment resteasyDeployment) {
        if (resteasyDeployment == null)
            return null;

        ServletInfo restServlet = Servlets.servlet("RESTEasy Servlet", HttpServlet30Dispatcher.class)
                .setAsyncSupported(true)
                .setLoadOnStartup(1)
                .addMapping("/*");

        DeploymentInfo deploymentInfo = new DeploymentInfo()
                .setDeploymentName("RESTEasy Deployment")
                .setContextPath(API_PATH)
                .addServletContextAttribute(ResteasyDeployment.class.getName(), resteasyDeployment)
                .addServlet(restServlet)
                .setClassLoader(Container.class.getClassLoader());

        if (identityService != null) {
            resteasyDeployment.setSecurityEnabled(true);
        } else {
            throw new RuntimeException("No identity service deployed, can't enable API security");
        }

        return addServletDeployment(identityService, deploymentInfo, resteasyDeployment.isSecurityEnabled());
    }

    protected HttpHandler createJsApiHandler(IdentityService identityService, ResteasyDeployment resteasyDeployment) {
        if (resteasyDeployment == null)
            return null;

        ServletInfo jsApiServlet = Servlets.servlet("RESTEasy JS Servlet", JSAPIServlet.class)
                .setAsyncSupported(true)
                .setLoadOnStartup(1)
                .addMapping("/*");

        DeploymentInfo deploymentInfo = new DeploymentInfo()
                .setDeploymentName("RESTEasy JS Deployment")
                .setContextPath(JSAPI_PATH)
                .addServlet(jsApiServlet)
                .setClassLoader(Container.class.getClassLoader());

        deploymentInfo.addServletContextAttribute(
                ResteasyContextParameters.RESTEASY_DEPLOYMENTS,
                new HashMap<String, ResteasyDeployment>() {{
                    put("", resteasyDeployment);
                }}
        );
        return addServletDeployment(identityService, deploymentInfo, false);
    }

    // TODO: Switch to use PathResourceManager
    public HttpHandler createFileHandler(boolean devMode, IdentityService identityService, Path filePath, String[] requiredRoles) {
        requiredRoles = requiredRoles == null ? new String[0] : requiredRoles;
        DeploymentInfo deploymentInfo = ManagerFileServlet.createDeploymentInfo(devMode, "", filePath, requiredRoles);
        return new CanonicalPathHandler(addServletDeployment(identityService, deploymentInfo, requiredRoles.length != 0));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "appDocRoot=" + appDocRoot +
                '}';
    }
}
