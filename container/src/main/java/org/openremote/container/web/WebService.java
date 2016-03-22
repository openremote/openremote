package org.openremote.container.web;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.RedirectHandler;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.*;
import io.undertow.util.HttpString;
import io.undertow.util.MimeMappings;
import org.jboss.resteasy.jsapi.JSAPIServlet;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.openremote.container.ConfigurationException;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.json.ElementalMessageBodyConverter;
import org.openremote.container.json.JacksonConfig;
import org.openremote.container.security.AuthOverloadHandler;
import org.openremote.container.security.SimpleKeycloakServletExtension;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static javax.ws.rs.core.HttpHeaders.CONTENT_ENCODING;
import static javax.ws.rs.core.UriBuilder.fromUri;

public abstract class WebService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(WebService.class.getName());

    public static final String WEBSERVER_LISTEN_HOST = "WEBSERVER_LISTEN_HOST";
    public static final String WEBSERVER_LISTEN_HOST_DEFAULT = "localhost";
    public static final String WEBSERVER_LISTEN_PORT = "WEBSERVER_LISTEN_PORT";
    public static final int WEBSERVER_LISTEN_PORT_DEFAULT = 8080;

    public static final String API_PATH = "/api";
    public static final String JSAPI_PATH = "/jsapi";
    public static final String STATIC_PATH = "/static";
    protected final Pattern PATTERN_STATIC = Pattern.compile(Pattern.quote(STATIC_PATH) + "(/.*)?");
    protected final Pattern PATTERN_REALM_ROOT = Pattern.compile("/([a-z]+)/?");
    protected final Pattern PATTERN_REALM_SUB = Pattern.compile("/([a-z]+)/(.*)");

    protected String host;
    protected int port;
    protected Undertow undertow;

    protected String defaultRealm;
    protected Map<String, HttpHandler> prefixRoutes = new LinkedHashMap<>();
    protected Path staticResourceDocRoot;
    protected Collection<Class<?>> apiClasses = new HashSet<>();
    protected Collection<Object> apiSingletons = new HashSet<>();
    protected KeycloakConfigResolver keycloakConfigResolver;

    @Override
    public void init(Container container) throws Exception {
        host = container.getConfig(WEBSERVER_LISTEN_HOST, WEBSERVER_LISTEN_HOST_DEFAULT);
        port = container.getConfigInteger(WEBSERVER_LISTEN_PORT, WEBSERVER_LISTEN_PORT_DEFAULT);
    }

    @Override
    public void configure(Container container) throws Exception {
        undertow = build(
            container,
            Undertow.builder()
                .addHttpListener(port, host)
        ).build();
    }

    @Override
    public void start(Container container) throws Exception {
        if (undertow != null) {
            LOG.info("Starting webserver on http://" + host + ":" + port);
            undertow.start();
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

        HttpHandler staticResourceHandler = getStaticResourceDocRoot() != null
            ? createStaticResourceHandler(container, getStaticResourceDocRoot())
            : null;

        ResteasyDeployment resteasyDeployment = createResteasyDeployment(container);
        HttpHandler apiHandler = createApiHandler(resteasyDeployment);
        HttpHandler jsApiHandler = createJsApiHandler(resteasyDeployment);

        HttpHandler handler = exchange -> {
            String requestPath = exchange.getRequestPath();
            LOG.fine("Handling request: " + exchange.getRequestMethod() + " " + exchange.getRequestURL());

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

            if (staticResourceHandler != null) {
                // Serve /<realm>/index.html
                Matcher realmRootMatcher = PATTERN_REALM_ROOT.matcher(requestPath);
                if (realmRootMatcher.matches()) {
                    LOG.fine("Serving index document of realm: " + requestPath);
                    exchange.setRelativePath("/index.html");
                    staticResourceHandler.handleRequest(exchange);
                    return;
                }

                // Serve static resources with path /static/*
                Matcher staticMatcher = PATTERN_STATIC.matcher(requestPath);
                if (staticMatcher.matches()) {
                    LOG.fine("Serving static resource: " + requestPath);
                    String remaining = staticMatcher.group(1);
                    exchange.setRelativePath(remaining == null || remaining.length() == 0 ? "/" : remaining);
                    // Special handling for compression of pbf static files (they are already compressed...)
                    // TODO configurable
                    if (exchange.getRequestPath().endsWith(".pbf")) {
                        exchange.getResponseHeaders().put(new HttpString(CONTENT_ENCODING), "gzip");
                    }
                    staticResourceHandler.handleRequest(exchange);
                    return;
                }
            }

            // Serve API with path /<realm>/*
            Matcher realmSubMatcher = PATTERN_REALM_SUB.matcher(requestPath);
            if (apiHandler != null && realmSubMatcher.matches()) {
                LOG.fine("Serving API call: " + requestPath);
                String realm = realmSubMatcher.group(1);

                // Move the realm from path segment to query parameter
                URI apiUrl = fromUri(exchange.getRequestURL())
                    .replacePath(API_PATH).path(realmSubMatcher.group(2))
                    .replaceQuery(exchange.getQueryString()).queryParam("realm", realm)
                    .build();

                exchange.setRequestURI(apiUrl.toString(), true);
                exchange.setRequestPath(apiUrl.getPath());
                exchange.setRelativePath(apiUrl.getPath());

                // This is probably the only actual source of query details for Resteasy
                exchange.setQueryString(apiUrl.getRawQuery());

                // Just to make it look nice
                exchange.addQueryParam("realm", realm);

                apiHandler.handleRequest(exchange);
                return;
            }

            LOG.fine("No resource found: " + requestPath);
            ResponseCodeHandler.HANDLE_404.handleRequest(exchange);
        };

        builder.setHandler(handler);

        return builder;
    }

    protected HttpHandler createStaticResourceHandler(Container container, Path docRoot) {
        if (!Files.isDirectory(docRoot))
            throw new ConfigurationException("Missing document root directory: " + docRoot.toAbsolutePath());
        LOG.info("Static document root directory: " + docRoot.toAbsolutePath());
        ResourceManager staticResourcesManager = new FileResourceManager(docRoot.toFile(), 0, true, false);

        MimeMappings.Builder mimeBuilder = MimeMappings.builder(true);
        mimeBuilder.addMapping("pbf", "application/x-protobuf");
        mimeBuilder.addMapping("wsdl", "application/xml");
        mimeBuilder.addMapping("xsl", "text/xsl");
        // TODO: Add more mime/magic stuff?

        return Handlers.resource(staticResourcesManager)
            .setMimeMappings(mimeBuilder.build());
    }

    protected HttpHandler createApiHandler(ResteasyDeployment resteasyDeployment) {
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

        if (getKeycloakConfigResolver() != null) {
            resteasyDeployment.setSecurityEnabled(true);
        }

        return addServletDeployment(deploymentInfo);
    }

    protected HttpHandler createJsApiHandler(ResteasyDeployment resteasyDeployment) {
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
        return addServletDeployment(deploymentInfo);
    }

    public HttpHandler addServletDeployment(DeploymentInfo deploymentInfo) {
        try {

            if (getKeycloakConfigResolver() != null) {
                deploymentInfo.addOuterHandlerChainWrapper(AuthOverloadHandler::new);
                deploymentInfo.setSecurityDisabled(false);
                LoginConfig loginConfig = new LoginConfig(SimpleKeycloakServletExtension.AUTH_MECHANISM, "OpenRemote");
                deploymentInfo.setLoginConfig(loginConfig);
                deploymentInfo.addServletExtension(new SimpleKeycloakServletExtension(getKeycloakConfigResolver()));
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

    protected ResteasyDeployment createResteasyDeployment(Container container) {
        if (getApiClasses() == null && getApiSingletons() == null)
            return null;
        WebApplication webApplication = new WebApplication(container, getApiClasses(), getApiSingletons());
        ResteasyDeployment resteasyDeployment = new ResteasyDeployment();
        resteasyDeployment.setApplication(webApplication);

        // Custom providers (these only apply to server applications, not client calls)
        resteasyDeployment.getProviders().add(new JacksonConfig(container));
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
     * Must be not-null to enable Keycloak extension.
     */
    public KeycloakConfigResolver getKeycloakConfigResolver() {
        return keycloakConfigResolver;
    }

    public void setKeycloakConfigResolver(KeycloakConfigResolver keycloakConfigResolver) {
        if (this.keycloakConfigResolver != null)
            throw new IllegalStateException("Keycloak config resolver already set: " + this.keycloakConfigResolver);
        this.keycloakConfigResolver = keycloakConfigResolver;
    }

    /**
     * Default realm path the browser will be redirected to when a / root request is made.
     */
    public String getDefaultRealm() {
        return defaultRealm;
    }

    public void setDefaultRealm(String defaultRealm) {
        if (this.defaultRealm != null)
            throw new IllegalStateException("Default realm already set: " + this.defaultRealm);
        this.defaultRealm = defaultRealm;
    }

    /**
     * Set to serve static resources from file system on the /static/* path.
     */
    public Path getStaticResourceDocRoot() {
        return staticResourceDocRoot;
    }

    public void setStaticResourceDocRoot(Path staticResourceDocRoot) {
        if (this.staticResourceDocRoot != null)
            throw new IllegalStateException("Static resource path already set: " + this.staticResourceDocRoot);
        this.staticResourceDocRoot = staticResourceDocRoot;
    }

}
