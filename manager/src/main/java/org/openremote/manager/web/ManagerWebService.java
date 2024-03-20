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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.*;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.servers.ServerVariable;
import io.swagger.v3.oas.models.servers.ServerVariables;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.CanonicalPathHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.RedirectHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.util.HttpString;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.openremote.container.security.IdentityService;
import org.openremote.container.web.WebService;
import org.openremote.model.Container;

import jakarta.ws.rs.WebApplicationException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.undertow.util.RedirectBuilder.redirect;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.UriBuilder.fromUri;
import static org.openremote.container.util.MapAccess.getString;
import static org.openremote.model.Constants.REALM_PARAM_NAME;
import static org.openremote.model.util.ValueUtil.configureObjectMapper;

public class ManagerWebService extends WebService {

    private static abstract class ServerVariableMixin {
        @JsonProperty("default")
        List<String> _default;
    }

    public static final int PRIORITY = LOW_PRIORITY + 100;
    public static final String OR_APP_DOCROOT = "OR_APP_DOCROOT";
    public static final String OR_APP_DOCROOT_DEFAULT = "manager/src/web";
    public static final String OR_CUSTOM_APP_DOCROOT = "OR_CUSTOM_APP_DOCROOT";
    public static final String OR_CUSTOM_APP_DOCROOT_DEFAULT = "deployment/manager/app";
    public static final String OR_ROOT_REDIRECT_PATH = "OR_ROOT_REDIRECT_PATH";
    public static final String OR_ROOT_REDIRECT_PATH_DEFAULT = "/manager";
    public static final String API_PATH = "/api";
    public static final String MANAGER_APP_PATH = "/manager";
    public static final String INSIGHTS_APP_PATH = "/insights";
    public static final String SWAGGER_APP_PATH = "/swagger";
    public static final String CONSOLE_LOADER_APP_PATH = "/console_loader";
    public static final String SHARED_PATH = "/shared";
    private static final Logger LOG = Logger.getLogger(ManagerWebService.class.getName());
    protected static final Pattern PATTERN_REALM_SUB = Pattern.compile("/([a-zA-Z0-9\\-_]+)/(.*)");

    protected boolean initialised;
    protected Path builtInAppDocRoot;
    protected Path customAppDocRoot;
    protected Collection<Class<?>> apiClasses = new HashSet<>();
    protected Collection<Object> apiSingletons = new HashSet<>();

    /**
     * Start web service after other services.
     */
    @Override
    public int getPriority() {
        return PRIORITY;
    }
    private static abstract class StringSchemaMixin {
        @JsonProperty("enum")
        protected List<String> _enum;
    }
    @Override
    public void init(Container container) throws Exception {
        super.init(container);

        String rootRedirectPath = getString(container.getConfig(), OR_ROOT_REDIRECT_PATH, OR_ROOT_REDIRECT_PATH_DEFAULT);

        // Modify swagger object mapper to match ours
        configureObjectMapper(Json.mapper());
        Json.mapper().addMixIn(StringSchema.class, StringSchemaMixin.class);
        Json.mapper().addMixIn(ServerVariable.class, ServerVariableMixin.class);

        // Add swagger resource
        OpenAPI oas = new OpenAPI()
            .servers(Collections.singletonList(new Server().url("/api/{realm}/").variables(new ServerVariables().addServerVariable("realm", new ServerVariable()._default("master")))))
                .schemaRequirement("openid", new SecurityScheme().type(SecurityScheme.Type.OAUTH2).flows(
                        new OAuthFlows().clientCredentials(
                                        // for service users
                                        new OAuthFlow()
                                                .refreshUrl("/auth/realms/master/protocol/openid-connect/token")
                                                .tokenUrl("/auth/realms/master/protocol/openid-connect/token")
                                                .scopes(new Scopes().addString("serviceUser", "serviceUser"))
                                )))
                .security(Collections.singletonList(new SecurityRequirement().addList("openid")));

        Info info = new Info()
            .title("OpenRemote Manager REST API")
            .version("1.0.0")
            .description("This is the documentation for the OpenRemote Manager HTTP REST API.  Please see the [wiki](https://github.com/openremote/openremote/wiki) for more info.")
            .contact(new Contact()
                .email("info@openremote.io"))
            .license(new License()
                .name("AGPL 3.0")
                .url("https://www.gnu.org/licenses/agpl-3.0.en.html"));

        oas.info(info);
        SwaggerConfiguration oasConfig = new SwaggerConfiguration()
            .resourcePackages(Stream.of("org.openremote.model.*").collect(Collectors.toSet()))
            .openAPI(oas);

        OpenApiResource openApiResource = new OpenApiResource();
        openApiResource.openApiConfiguration(oasConfig);
        addApiSingleton(openApiResource);

        initialised = true;
        ResteasyDeployment resteasyDeployment = createResteasyDeployment(container, getApiClasses(), apiSingletons, true);

        // Serve REST API
        HttpHandler apiHandler = createApiHandler(container, resteasyDeployment);

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
                exchange.getRequestHeaders().put(HttpString.tryFromString(REALM_PARAM_NAME), realm);

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
        builtInAppDocRoot = Paths.get(getString(container.getConfig(), OR_APP_DOCROOT, OR_APP_DOCROOT_DEFAULT));
        customAppDocRoot = Paths.get(getString(container.getConfig(), OR_CUSTOM_APP_DOCROOT, OR_CUSTOM_APP_DOCROOT_DEFAULT));

        HttpHandler defaultHandler = null;

        if (Files.isDirectory(customAppDocRoot)) {
            HttpHandler customBaseFileHandler = createFileHandler(container, customAppDocRoot, null);
            defaultHandler = exchange -> {
                if (exchange.getRelativePath().isEmpty() || "/".equals(exchange.getRelativePath())) {
                    exchange.setRelativePath("/index.html");
                }
                customBaseFileHandler.handleRequest(exchange);
            };
        }

        PathHandler deploymentHandler = defaultHandler != null ? new PathHandler(defaultHandler) : new PathHandler();

        // Serve deployment files
        if (Files.isDirectory(builtInAppDocRoot)) {
            HttpHandler appBaseFileHandler = createFileHandler(container, builtInAppDocRoot, null);
            HttpHandler appFileHandler = exchange -> {
                if (exchange.getRelativePath().isEmpty() || "/".equals(exchange.getRelativePath())) {
                    exchange.setRelativePath("/index.html");
                }

                // Reinstate the full path
                exchange.setRelativePath(exchange.getRequestPath());
                appBaseFileHandler.handleRequest(exchange);
            };

            deploymentHandler.addPrefixPath(MANAGER_APP_PATH, appFileHandler);
            deploymentHandler.addPrefixPath(INSIGHTS_APP_PATH, appFileHandler);
            deploymentHandler.addPrefixPath(SWAGGER_APP_PATH, appFileHandler);
            deploymentHandler.addPrefixPath(CONSOLE_LOADER_APP_PATH, appFileHandler);
            deploymentHandler.addPrefixPath(SHARED_PATH, appFileHandler);
        }

        // Add all route handlers required by the manager in priority order

        // Redirect / to default app
        if (rootRedirectPath != null) {
            getRequestHandlers().add(
                new RequestHandler(
                    "Default app redirect",
                    exchange -> exchange.getRequestPath().equals("/"),
                    exchange -> {
                        LOG.finest("Handling root request, redirecting client to default app");
                        new RedirectHandler(redirect(exchange, rootRedirectPath)).handleRequest(exchange);
                    }));
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
    public void addApiSingleton(Object singleton) {
        if (this.initialised) {
            throw new IllegalStateException("API singletons must be added before the service is initialised");
        }
        apiSingletons.add(singleton);
    }

    public Path getBuiltInAppDocRoot() {
        return builtInAppDocRoot;
    }

    public Path getCustomAppDocRoot() {
        return customAppDocRoot;
    }

    protected HttpHandler createApiHandler(Container container, ResteasyDeployment resteasyDeployment) {
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

        IdentityService identityService = container.getService(IdentityService.class);

        if (identityService != null) {
            resteasyDeployment.setSecurityEnabled(true);
        } else {
            throw new RuntimeException("No identity service deployed, can't enable API security");
        }

        return addServletDeployment(container, deploymentInfo, resteasyDeployment.isSecurityEnabled());
    }

    // TODO: Switch to use PathResourceManager
    public static HttpHandler createFileHandler(Container container, Path filePath, String[] requiredRoles) {
        boolean devMode = container.isDevMode();
        requiredRoles = requiredRoles == null ? new String[0] : requiredRoles;
        DeploymentInfo deploymentInfo = ManagerFileServlet.createDeploymentInfo(devMode, "", filePath, requiredRoles);
        return new CanonicalPathHandler(addServletDeployment(container, deploymentInfo, requiredRoles.length != 0));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "builtInAppDocRoot=" + builtInAppDocRoot +
            ", customAppDocRoot=" + customAppDocRoot +
            '}';
    }
}
