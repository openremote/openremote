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
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.RequestDumpingHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.util.ImmediateInstanceHandle;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.openremote.container.json.JacksonConfig;
import org.openremote.container.security.CORSFilter;
import org.openremote.container.security.IdentityService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.util.TextUtil;
import org.xnio.Options;

import javax.servlet.DispatcherType;
import javax.ws.rs.core.UriBuilder;
import java.net.Inet4Address;
import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.openremote.container.util.MapAccess.*;
import static org.openremote.model.Constants.OR_ADDITIONAL_HOSTNAMES;
import static org.openremote.model.Constants.OR_HOSTNAME;

public abstract class WebService implements ContainerService {

    public static class RequestHandler {
        protected String name;
        protected Predicate<HttpServerExchange> handlePredicate;
        protected HttpHandler handler;

        public RequestHandler(String name, Predicate<HttpServerExchange> handlePredicate, HttpHandler handler) {
            this.name = name;
            this.handlePredicate = handlePredicate;
            this.handler = handler;
        }

        public String getName() {
            return name;
        }

        public Predicate<HttpServerExchange> getHandlePredicate() {
            return handlePredicate;
        }

        public io.undertow.server.HttpHandler getHandler() {
            return handler;
        }
    }

    // Change this to 0.0.0.0 to bind on all interfaces, enabling
    // access of the manager service from other devices in your LAN
    public static final String OR_WEBSERVER_LISTEN_HOST = "OR_WEBSERVER_LISTEN_HOST";
    public static final String OR_WEBSERVER_LISTEN_HOST_DEFAULT = "0.0.0.0";
    public static final String OR_WEBSERVER_LISTEN_PORT = "OR_WEBSERVER_LISTEN_PORT";
    public static final int OR_WEBSERVER_LISTEN_PORT_DEFAULT = 8080;
    public static final String OR_WEBSERVER_DUMP_REQUESTS = "OR_WEBSERVER_DUMP_REQUESTS";
    public static final boolean OR_WEBSERVER_DUMP_REQUESTS_DEFAULT = false;
    public static final String OR_WEBSERVER_ALLOWED_ORIGINS = "OR_WEBSERVER_ALLOWED_ORIGINS";
    public static final String OR_WEBSERVER_IO_THREADS_MAX = "OR_WEBSERVER_IO_THREADS_MAX";
    public static final int OR_WEBSERVER_IO_THREADS_MAX_DEFAULT = Math.max(Runtime.getRuntime().availableProcessors(), 2);
    public static final String OR_WEBSERVER_WORKER_THREADS_MAX = "OR_WEBSERVER_WORKER_THREADS_MAX";
    public static final int WEBSERVER_WORKER_THREADS_MAX_DEFAULT = Math.max(Runtime.getRuntime().availableProcessors(), 10);
    private static final Logger LOG = Logger.getLogger(WebService.class.getName());
    protected static AtomicReference<CORSFilter> corsFilterRef;
    protected boolean devMode;
    protected String host;
    protected int port;
    protected Undertow undertow;
    protected List<RequestHandler> httpHandlers = new ArrayList<>();
    protected URI containerHostUri;

    protected static String getLocalIpAddress() throws Exception {
        return Inet4Address.getLocalHost().getHostAddress();
    }

    public static RequestHandler pathStartsWithHandler(String name, String path, HttpHandler handler) {
        return new RequestHandler(name, exchange -> exchange.getRequestPath().startsWith(path), handler);
    }

    @Override
    public void init(Container container) throws Exception {
        devMode = container.isDevMode();
        host = getString(container.getConfig(), OR_WEBSERVER_LISTEN_HOST, OR_WEBSERVER_LISTEN_HOST_DEFAULT);
        port = getInteger(container.getConfig(), OR_WEBSERVER_LISTEN_PORT, OR_WEBSERVER_LISTEN_PORT_DEFAULT);
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
                        .setIoThreads(getInteger(container.getConfig(), OR_WEBSERVER_IO_THREADS_MAX, OR_WEBSERVER_IO_THREADS_MAX_DEFAULT))
                        .setWorkerThreads(getInteger(container.getConfig(), OR_WEBSERVER_WORKER_THREADS_MAX, WEBSERVER_WORKER_THREADS_MAX_DEFAULT))
                        .setWorkerOption(Options.WORKER_NAME, "WebService")
                        .setWorkerOption(Options.THREAD_DAEMON, true)
        ).build();
    }

    @Override
    public void start(Container container) throws Exception {
        if (undertow != null) {
            undertow.start();
            LOG.info("Webserver ready on http://" + host + ":" + port);
        }
    }

    @Override
    public void stop(Container container) throws Exception {
        if (undertow != null) {
            undertow.stop();
            undertow = null;
        }
    }

    /**
     * Adds a deployment to the default servlet container and returns the started handler.
     */
    public static HttpHandler addServletDeployment(Container container, DeploymentInfo deploymentInfo, boolean secure) {

        IdentityService identityService = container.getService(IdentityService.class);
        boolean devMode = container.isDevMode();

        try {
            if (secure) {
                if (identityService == null)
                    throw new IllegalStateException(
                            "No identity service found, make sure " + IdentityService.class.getName() + " is added before this service"
                    );
                identityService.secureDeployment(deploymentInfo);
            } else {
                LOG.info("Deploying insecure web context: " + deploymentInfo.getContextPath());
            }

            // This will catch anything not handled by Resteasy/Servlets, such as IOExceptions "at the wrong time"
            deploymentInfo.setExceptionHandler(new WebServiceExceptions.ServletUndertowExceptionHandler(devMode));

            // Add CORS filter that works for any servlet deployment
            FilterInfo corsFilterInfo = getCorsFilterInfo(container);

            if (corsFilterInfo != null) {
                deploymentInfo.addFilter(corsFilterInfo);
                deploymentInfo.addFilterUrlMapping(corsFilterInfo.getName(), "*", DispatcherType.REQUEST);
                deploymentInfo.addFilterUrlMapping(corsFilterInfo.getName(), "*", DispatcherType.FORWARD);
            }

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

    /**
     * When a request comes in the handlers are called in order until a handler returns a non null value; when this
     * happens the returned {@link RequestHandler} is invoked.
     */
    public List<RequestHandler> getRequestHandlers() {
        return this.httpHandlers;
    }

    /**
     * Provides the LAN IPv4 address the container is bound to so it can be
     * used in the context provider callbacks; if CB is on the other side of some sort
     * of NAT then this won't work also assumes HTTP
     */
    public URI getHostUri() {
        return containerHostUri;
    }

    protected Undertow.Builder build(Container container, Undertow.Builder builder) {

        LOG.info("Building web routing with handler(s): " + getRequestHandlers().stream().map(h -> h.name).collect(Collectors.joining("\n")));

        HttpHandler handler = exchange -> {
            String requestPath = exchange.getRequestPath();
            LOG.fine("Handling request: " + exchange.getRequestMethod() + " " + exchange.getRequestPath());

            boolean handled = false;
            for (RequestHandler requestHandler : getRequestHandlers()) {
                if (requestHandler.handlePredicate.test(exchange)) {
                    LOG.fine("Handling '" + requestPath + "' with handler: " + requestHandler.name);
                    requestHandler.handler.handleRequest(exchange);
                    handled = true;
                    break;
                }
            }

            if (!handled) {
                LOG.warning("No handler found for request: " + exchange.getRequestPath());
            }
        };

        handler = new WebServiceExceptions.RootUndertowExceptionHandler(devMode, handler);

        if (getBoolean(container.getConfig(), OR_WEBSERVER_DUMP_REQUESTS, OR_WEBSERVER_DUMP_REQUESTS_DEFAULT)) {
            handler = new RequestDumpingHandler(handler);
        }

        builder.setHandler(handler);

        return builder;
    }

    protected ResteasyDeployment createResteasyDeployment(Container container, Collection<Class<?>> apiClasses, Collection<Object> apiSingletons, boolean secure) {
        if (apiClasses == null && apiSingletons == null)
            return null;
        WebApplication webApplication = new WebApplication(container, apiClasses, apiSingletons);
        ResteasyDeployment resteasyDeployment = new ResteasyDeployment();
        resteasyDeployment.setApplication(webApplication);

        // Custom providers (these only apply to server applications, not client calls)
        resteasyDeployment.getProviders().add(new WebServiceExceptions.DefaultResteasyExceptionMapper(devMode));
        resteasyDeployment.getProviders().add(new WebServiceExceptions.ForbiddenResteasyExceptionMapper(devMode));
        resteasyDeployment.getProviders().add(new JacksonConfig());

        resteasyDeployment.getProviders().add(new GZIPEncodingInterceptor(!container.isDevMode()));
        resteasyDeployment.getActualProviderClasses().add(AlreadyGzippedWriterInterceptor.class);
        resteasyDeployment.getActualProviderClasses().add(ClientErrorExceptionHandler.class);

        resteasyDeployment.setSecurityEnabled(secure);

        return resteasyDeployment;
    }

    public static synchronized FilterInfo getCorsFilterInfo(Container container) {

        if (corsFilterRef == null) {
            CORSFilter corsFilter = null;

            if (!container.isDevMode()) {
                Set<String> allowedOrigins = getAllowedOrigins(container);

                if (!allowedOrigins.isEmpty()) {
                    corsFilter = new CORSFilter();
                    corsFilter.setAllowCredentials(true);
                    corsFilter.setAllowedMethods("GET, POST, PUT, DELETE, OPTIONS, HEAD");
                    corsFilter.setExposedHeaders("*");
                    corsFilter.setCorsMaxAge(1209600);
                    corsFilter.getAllowedOrigins().addAll(allowedOrigins);
                }
            } else {
                corsFilter = new CORSFilter();
                corsFilter.getAllowedOrigins().add("*");
                corsFilter.setAllowCredentials(true);
                corsFilter.setExposedHeaders("*");
                corsFilter.setAllowedMethods("GET, POST, PUT, DELETE, OPTIONS, HEAD");
                corsFilter.setCorsMaxAge(1209600);
            }

            corsFilterRef = new AtomicReference<>(corsFilter);
        }

        if (corsFilterRef.get() != null) {
            CORSFilter finalCorsFilter = corsFilterRef.get();
            return Servlets.filter("CORS Filter", CORSFilter.class, () -> new ImmediateInstanceHandle<>(finalCorsFilter))
                .setAsyncSupported(true);
        }

        return null;
    }

    public static List<String> getExternalHostnames(Container container) {

        // Get list of external hostnames
        String defaultHostname = getString(container.getConfig(), OR_HOSTNAME, null);
        String additionalHostnamesStr = getString(container.getConfig(), OR_ADDITIONAL_HOSTNAMES, null);

        List<String> externalHostnames = new ArrayList<>();

        if (!TextUtil.isNullOrEmpty(additionalHostnamesStr)) {
            externalHostnames.addAll(Arrays.stream(additionalHostnamesStr.split(","))
                .toList());
        }

        if (!TextUtil.isNullOrEmpty(defaultHostname) && !externalHostnames.contains(defaultHostname)) {
            externalHostnames.add(defaultHostname);
        }

        return externalHostnames;
    }

    public static Set<String> getAllowedOrigins(Container container) {
        // Set allowed origins using external hostnames and WEBSERVER_ALLOWED_ORIGINS
        HashSet<String> allowedOrigins = new HashSet<>(
            getExternalHostnames(container)
                .stream().map(hostname -> "https://" + hostname).toList()
        );
        String allowedOriginsStr = getString(container.getConfig(), OR_WEBSERVER_ALLOWED_ORIGINS, null);

        if (!TextUtil.isNullOrEmpty(allowedOriginsStr)) {
            allowedOrigins.addAll(Arrays.stream(allowedOriginsStr.split(",")).toList());
        }

        return allowedOrigins;
    }
}
