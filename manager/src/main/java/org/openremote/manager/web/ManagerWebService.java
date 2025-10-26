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
import io.undertow.server.handlers.RedirectHandler;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.servlet.api.DeploymentInfo;
import jakarta.ws.rs.core.Application;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.openremote.container.web.CompositeResourceManager;
import org.openremote.container.web.WebApplication;
import org.openremote.container.web.WebService;
import org.openremote.model.Container;
import org.openremote.model.util.TextUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.openremote.container.util.MapAccess.getString;
import static org.openremote.model.util.ValueUtil.configureObjectMapper;

public class ManagerWebService extends WebService {

    private static abstract class ServerVariableMixin {
        @JsonProperty("default")
        List<String> _default;
    }

    private static abstract class StringSchemaMixin {
        @JsonProperty("enum")
        protected List<String> _enum;
    }

    public static final int PRIORITY = LOW_PRIORITY + 100;
    public static final String OR_APP_DOCROOT = "OR_APP_DOCROOT";
    public static final String OR_APP_DOCROOT_DEFAULT = "ui/app";
    public static final String OR_CUSTOM_APP_DOCROOT = "OR_CUSTOM_APP_DOCROOT";
    public static final String OR_CUSTOM_APP_DOCROOT_DEFAULT = "deployment/manager/app";
    public static final String OR_ROOT_REDIRECT_PATH = "OR_ROOT_REDIRECT_PATH";
    public static final String OR_ROOT_REDIRECT_PATH_DEFAULT = "/manager";

    public static final String API_PATH = "/api";
    private static final Logger LOG = Logger.getLogger(ManagerWebService.class.getName());
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

    @Override
    public void init(Container container) throws Exception {
        super.init(container);

        builtInAppDocRoot = Paths.get(getString(container.getConfig(), OR_APP_DOCROOT, OR_APP_DOCROOT_DEFAULT));
        customAppDocRoot = Paths.get(getString(container.getConfig(), OR_CUSTOM_APP_DOCROOT, OR_CUSTOM_APP_DOCROOT_DEFAULT));
        String rootRedirectPath = getString(container.getConfig(), OR_ROOT_REDIRECT_PATH, OR_ROOT_REDIRECT_PATH_DEFAULT);

       // Add a handler to redirect requests for the exact root path "/" to the configured default path
       if (!TextUtil.isNullOrEmpty(rootRedirectPath)) {
          LOG.info("Adding root redirect to: " + rootRedirectPath);
          pathHandler.addExactPath("/", new RedirectHandler(rootRedirectPath));
       }

       addOpenApiResource();

        initialised = true;

        // Serve REST API
       Collection<Object> deploymentSingletons = Stream.of(
          devMode ? getStandardProviders(devMode, 0) : getStandardProviders(devMode, 0,
             getCORSAllowedOrigins(container),
             getString(container.getConfig(), OR_WEBSERVER_ALLOWED_METHODS, DEFAULT_CORS_ALLOW_ALL),
             getString(container.getConfig(), OR_WEBSERVER_EXPOSED_HEADERS, DEFAULT_CORS_ALLOW_ALL),
             DEFAULT_CORS_MAX_AGE,
             DEFAULT_CORS_ALLOW_CREDENTIALS),
          apiSingletons).flatMap(Collection::stream).toList();

        Application APIApplication = new WebApplication(
                container,
               apiClasses,
               deploymentSingletons);

       ResteasyDeployment deployment = createResteasyDeployment(APIApplication, true);
       DeploymentInfo deploymentInfo = createDeploymentInfo(deployment, API_PATH, "Manager HTTP API", devMode, true);
       deploy(deploymentInfo, true, false);

       // Deploy static app files unsecured
       ResourceManager filesResourceManager = new PathResourceManager(builtInAppDocRoot);

       // If custom app docroot is a directory then make it the default file handler
        if (customAppDocRoot != null && Files.isDirectory(customAppDocRoot)) {
            ResourceManager customAppResourceManager = new PathResourceManager(customAppDocRoot);
            filesResourceManager = new CompositeResourceManager(filesResourceManager, customAppResourceManager);
        } else if (customAppDocRoot != null) {
           LOG.info("Custom app doc root does not exist: " + customAppDocRoot.toAbsolutePath());
        }

        deploymentInfo = createFilesDeploymentInfo(filesResourceManager, "/", "App Files", devMode, null);
        deploy(deploymentInfo, false, true);
    }

   private void addOpenApiResource() {
        // Modify swagger object mapper to match ours
        configureObjectMapper(Json.mapper());
        Json.mapper().addMixIn(StringSchema.class, StringSchemaMixin.class);
        Json.mapper().addMixIn(ServerVariable.class, ServerVariableMixin.class);

        // Add swagger resource
        OpenAPI oas = new OpenAPI()
                .servers(List.of(new Server().url("/api/{realm}/").variables(new ServerVariables().addServerVariable("realm", new ServerVariable()._default("master")))))
                .schemaRequirement("openid", new SecurityScheme().type(SecurityScheme.Type.OAUTH2).flows(
                        new OAuthFlows() //
                                .authorizationCode(
                                        new OAuthFlow()
                                                .authorizationUrl("/auth/realms/master/protocol/openid-connect/auth")
                                                .refreshUrl("/auth/realms/master/protocol/openid-connect/token")
                                                .tokenUrl("/auth/realms/master/protocol/openid-connect/token")
                                                .scopes(new Scopes().addString("profile", "profile"))
                                )
                                .clientCredentials(
                                        // for service users
                                        new OAuthFlow()
                                                .tokenUrl("/auth/realms/master/protocol/openid-connect/token")
                                                .refreshUrl("/auth/realms/master/protocol/openid-connect/token")
                                                .scopes(new Scopes().addString("profile", "profile"))
                                )
                )).security(List.of(new SecurityRequirement().addList("openid")));

        Info info = new Info()
                .title("OpenRemote Manager HTTP API")
                .version("3.0.0")
                .description("This is the documentation for the OpenRemote Manager HTTP API.  Please see the [documentation](https://docs.openremote.io) for more info.")
                .contact(new Contact().email("info@openremote.io"))
                .license(new License().name("AGPL 3.0").url("https://www.gnu.org/licenses/agpl-3.0.en.html"));

        oas.info(info);
        SwaggerConfiguration oasConfig = new SwaggerConfiguration()
                .resourcePackages(Set.of("org.openremote.model.*"))
                .openAPI(oas)
                .defaultResponseCode("200");
        OpenApiResource openApiResource = new OpenApiResource();
        openApiResource.openApiConfiguration(oasConfig);
        addApiSingleton(openApiResource);
    }

    /**
     * Add resource/provider/etc. classes to enable REST API
     */
    public void addApiClasses(Class<?> apiClass) {
       if (this.initialised) {
          throw new IllegalStateException("API classes must be added before the service is initialised");
       }
       apiClasses.add(apiClass);
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

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "builtInAppDocRoot=" + builtInAppDocRoot +
                ", customAppDocRoot=" + customAppDocRoot +
                '}';
    }
}
