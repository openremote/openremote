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
package org.openremote.container.web;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.RedirectHandler;
import io.undertow.server.handlers.RequestDumpingHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.LoginConfig;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.util.HttpString;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.json.ElementalMessageBodyConverter;
import org.openremote.container.json.JacksonConfig;
import org.openremote.container.security.AuthOverloadHandler;
import org.openremote.container.security.CORSFilter;
import org.openremote.container.security.IdentityService;
import org.openremote.container.security.SimpleKeycloakServletExtension;
import org.openremote.container.web.jsapi.JSAPIServlet;
import org.xnio.Options;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.UriBuilder;
import java.net.Inet4Address;
import java.net.URI;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.UriBuilder.fromUri;
import static org.openremote.container.util.MapAccess.*;

public abstract class WebService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(WebService.class.getName());

    public static final String WEBSERVER_LISTEN_HOST = "WEBSERVER_LISTEN_HOST";
    public static final String WEBSERVER_LISTEN_HOST_DEFAULT = "0.0.0.0";
    public static final String WEBSERVER_LISTEN_PORT = "WEBSERVER_LISTEN_PORT";
    public static final int WEBSERVER_LISTEN_PORT_DEFAULT = 8080;
    public static final String WEBSERVER_DUMP_REQUESTS = "WEBSERVER_DUMP_REQUESTS";
    public static final boolean WEBSERVER_DUMP_REQUESTS_DEFAULT = false;
    public static final String WEBSERVER_IO_THREADS_MAX = "WEBSERVER_IO_THREADS_MAX";
    public static final int WEBSERVER_IO_THREADS_MAX_DEFAULT = Math.max(Runtime.getRuntime().availableProcessors(), 2);
    public static final String WEBSERVER_WORKER_THREADS_MAX = "WEBSERVER_WORKER_THREADS_MAX";
    public static final int WEBSERVER_WORKER_THREADS_MAX_DEFAULT = Math.max(Runtime.getRuntime().availableProcessors(), 10);

    // Authenticating requests requires a realm, either we receive this in a header or
    // we extract it (e.g. from request path segment) and set it as a header before
    // processing the request
    public static final String REQUEST_HEADER_REALM = "Auth-Realm";

    public static final String API_PATH = "/api";
    public static final String JSAPI_PATH = "/jsapi";
    protected final Pattern PATTERN_REALM_ROOT = Pattern.compile("/([a-zA-Z0-9\\-_]+)/?");
    protected final Pattern PATTERN_REALM_SUB = Pattern.compile("/([a-zA-Z0-9\\-_]+)/(.*)");

    protected boolean devMode;
    protected String host;
    protected int port;
    protected Undertow undertow;

    protected Map<String, HttpHandler> prefixRoutes = new LinkedHashMap<>();
    protected Collection<Class<?>> apiClasses = new HashSet<>();
    protected Collection<Object> apiSingletons = new HashSet<>();
    protected URI containerHostUri;

    protected static String getLocalIpAddress() throws Exception {
        return Inet4Address.getLocalHost().getHostAddress();
    }

    @Override
    public void init(Container container) throws Exception {
        devMode = container.isDevMode();
        host = getString(container.getConfig(), WEBSERVER_LISTEN_HOST, WEBSERVER_LISTEN_HOST_DEFAULT);
        port = getInteger(container.getConfig(), WEBSERVER_LISTEN_PORT, WEBSERVER_LISTEN_PORT_DEFAULT);
        String containerHost = host.equalsIgnoreCase("localhost") || host.indexOf("127") == 0 || host.indexOf("0.0.0.0") == 0
            ? getLocalIpAddress()
            : host;

        containerHostUri =
            UriBuilder.fromPath("/")
                .scheme("http")
                .host(containerHost)
                .port(port).build();


        undertow = build(
            container,
            Undertow.builder()
                .addHttpListener(port, host)
                .setIoThreads(getInteger(container.getConfig(), WEBSERVER_IO_THREADS_MAX, WEBSERVER_IO_THREADS_MAX_DEFAULT))
                .setWorkerThreads(getInteger(container.getConfig(), WEBSERVER_WORKER_THREADS_MAX, WEBSERVER_WORKER_THREADS_MAX_DEFAULT))
                .setWorkerOption(Options.WORKER_NAME, "WebService")
                .setWorkerOption(Options.THREAD_DAEMON, true)
        ).build();
    }

    @Override
    public void start(Container container) throws Exception {
        if (undertow != null) {
            LOG.info("Starting webserver on http://" + host + ":" + port);
            undertow.start();
            LOG.info("Webserver ready on http://" + host + ":" + port);
        }
    }

    @Override
    public void stop(Container container) throws Exception {
        if (undertow != null) {
            LOG.info("Stopping webserver...");
            undertow.stop();
            undertow = null;
        }
    }

    protected Undertow.Builder build(Container container, Undertow.Builder builder) {

        LOG.info("Building web routing with custom routes: " + getPrefixRoutes().keySet());

        IdentityService identityService = container.hasService(IdentityService.class)
            ? container.getService(IdentityService.class)
            : null;

        ResteasyDeployment resteasyDeployment = createResteasyDeployment(container);
        HttpHandler apiHandler = createApiHandler(identityService, resteasyDeployment);
        HttpHandler jsApiHandler = createJsApiHandler(identityService, resteasyDeployment);

        HttpHandler handler = exchange -> {
            String requestPath = exchange.getRequestPath();
            LOG.fine("Handling request: " + exchange.getRequestMethod() + " " + exchange.getRequestPath());

            // Other services can register routes here with a prefix patch match
            boolean handled = false;
            for (Map.Entry<String, HttpHandler> entry : getPrefixRoutes().entrySet()) {
                if (requestPath.startsWith(entry.getKey())) {
                    LOG.fine("Handling with '" + entry.getValue().getClass().getName() + "' path prefix: " + entry.getKey());
                    entry.getValue().handleRequest(exchange);
                    handled = true;
                    break;
                }
            }
            if (handled)
                return;

            // Redirect / to default realm
            if (requestPath.equals("/")) {
                LOG.fine("Handling root request, redirecting client to default realm: " + requestPath);
                new RedirectHandler(
                    fromUri(exchange.getRequestURL()).replacePath(getDefaultRealm()).build().toString()
                ).handleRequest(exchange);
                return;
            }

            // Serve JavaScript API with path /jsapi/*
            if (jsApiHandler != null && requestPath.startsWith(JSAPI_PATH)) {
                LOG.fine("Serving JS API call: " + requestPath);
                jsApiHandler.handleRequest(exchange);
                return;
            }

            // Serve /<realm>/index.html
            Matcher realmRootMatcher = PATTERN_REALM_ROOT.matcher(requestPath);
            if (getRealmIndexHandler() != null && realmRootMatcher.matches()) {
                LOG.fine("Serving index document of realm: " + requestPath);
                exchange.setRelativePath("/index.html");
                getRealmIndexHandler().handleRequest(exchange);
                return;
            }

            // Serve API with path /<realm>/*
            Matcher realmSubMatcher = PATTERN_REALM_SUB.matcher(requestPath);
            if (apiHandler != null && realmSubMatcher.matches()) {
                LOG.fine("Serving API call: " + requestPath);
                String realm = realmSubMatcher.group(1);

                // Move the realm from path segment to header
                exchange.getRequestHeaders().put(HttpString.tryFromString(REQUEST_HEADER_REALM), realm);

                // Rewrite path, remove realm segment
                URI apiUrl = fromUri(exchange.getRequestURL())
                    .replacePath(API_PATH).path(realmSubMatcher.group(2))
                    .build();
                exchange.setRequestURI(apiUrl.toString(), true);
                exchange.setRequestPath(apiUrl.getPath());
                exchange.setRelativePath(apiUrl.getPath());

                apiHandler.handleRequest(exchange);
                return;
            }

            exchange.setStatusCode(NOT_FOUND.getStatusCode());
            throw new WebApplicationException(NOT_FOUND);
        };

        handler = new WebServiceExceptions.RootUndertowExceptionHandler(devMode, handler);

        if (getBoolean(container.getConfig(), WEBSERVER_DUMP_REQUESTS, WEBSERVER_DUMP_REQUESTS_DEFAULT)) {
            handler = new RequestDumpingHandler(handler);
        }

        builder.setHandler(handler);

        return builder;
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

        if (identityService != null && identityService.getKeycloakConfigResolver() != null) {
            resteasyDeployment.setSecurityEnabled(true);
        }

        return addServletDeployment(identityService, deploymentInfo, true);
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

    /**
     * Adds a deployment to the default servlet container and returns the started handler.
     *
     * @param identityService Must not be null if secure deployment is used, source of configuration for the Keycloak extension
     * @param deploymentInfo  The deployment to add to the default container
     * @param secure          If Keycloak extension should be enabled for this deployment
     */
    public HttpHandler addServletDeployment(IdentityService identityService, DeploymentInfo deploymentInfo, boolean secure) {
        try {
            if (secure) {
                if (identityService == null)
                    throw new IllegalStateException(
                        "No identity service found, make sure " + IdentityService.class.getName() + " is added before this service"
                    );

                KeycloakConfigResolver keycloakConfigResolver = identityService.getKeycloakConfigResolver();
                if (keycloakConfigResolver == null)
                    throw new IllegalStateException(
                        "No Keycloak config resolver found, make sure " + IdentityService.class.getName() + " is initialized"
                    );

                LOG.info("Deploying secure web context: " + deploymentInfo.getContextPath());
                deploymentInfo.addOuterHandlerChainWrapper(AuthOverloadHandler::new);
                deploymentInfo.setSecurityDisabled(false);
                LoginConfig loginConfig = new LoginConfig(SimpleKeycloakServletExtension.AUTH_MECHANISM, "OpenRemote");
                deploymentInfo.setLoginConfig(loginConfig);
                deploymentInfo.addServletExtension(new SimpleKeycloakServletExtension(keycloakConfigResolver));
            } else {
                LOG.info("Deploying insecure web context: " + deploymentInfo.getContextPath());
            }

            // This will catch anything not handled by Resteasy/Servlets, such as IOExceptions "at the wrong time"
            deploymentInfo.setExceptionHandler(new WebServiceExceptions.ServletUndertowExceptionHandler(devMode));

            DeploymentManager manager = Servlets.defaultContainer().addDeployment(deploymentInfo);
            manager.deploy();
            return manager.start();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void removeServletDeployment(DeploymentInfo deploymentInfo) {
        try {
            DeploymentManager manager = Servlets.defaultContainer().getDeployment(deploymentInfo.getDeploymentName());
            manager.stop();
            manager.undeploy();
            Servlets.defaultContainer().removeDeployment(deploymentInfo);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    protected ResteasyDeployment createResteasyDeployment(Container container) {
        if (getApiClasses() == null && getApiSingletons() == null)
            return null;
        WebApplication webApplication = new WebApplication(container, getApiClasses(), getApiSingletons());
        ResteasyDeployment resteasyDeployment = new ResteasyDeployment();
        resteasyDeployment.setApplication(webApplication);

        // Custom providers (these only apply to server applications, not client calls)
        resteasyDeployment.getProviders().add(new WebServiceExceptions.DefaultResteasyExceptionMapper(devMode));
        resteasyDeployment.getProviders().add(new WebServiceExceptions.ForbiddenResteasyExceptionMapper(devMode));
        resteasyDeployment.getProviders().add(new JacksonConfig());
        resteasyDeployment.getProviders().add(new CORSFilter());
        resteasyDeployment.getActualProviderClasses().add(ElementalMessageBodyConverter.class);
        resteasyDeployment.getActualProviderClasses().add(AlreadyGzippedWriterInterceptor.class);
        resteasyDeployment.getActualProviderClasses().add(ClientErrorExceptionHandler.class);

        return resteasyDeployment;
    }

    /**
     * Add handlers with a path prefix.
     */
    public Map<String, HttpHandler> getPrefixRoutes() {
        return prefixRoutes;
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

    /**
     * Provides the LAN IPv4 address the container is bound to so it can be
     * used in the context provider callbacks; if CB is on the other side of some sort
     * of NAT then this won't work also assumes HTTP
     *
     * @return
     */
    public URI getHostUri() {
        return containerHostUri;
    }

    /**
     * Default realm path the browser will be redirected to when a / root request is made.
     */
    abstract protected String getDefaultRealm();

    /**
     * @return Optional handler that can handle the request to /<realm>/index.html
     */
    abstract protected HttpHandler getRealmIndexHandler();

}
