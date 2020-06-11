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
package org.openremote.container.security;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.util.HttpString;
import org.jboss.resteasy.spi.CorsHeaders;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.security.keycloak.KeycloakIdentityProvider;

import javax.ws.rs.core.UriBuilder;
import java.util.logging.Logger;

import static org.openremote.container.util.MapAccess.*;

/**
 * Needs to run before persistence service because if BASIC identity provider is configured then flyway must be
 * configured to manage PUBLIC schema for user data; that configuration is done by the
 * {@link org.openremote.container.security.basic.BasicIdentityProvider} and must happen before flyway initialisation.
 */
public abstract class IdentityService implements ContainerService {

    public static final int PRIORITY = PersistenceService.PRIORITY - 10;
    private static final Logger LOG = Logger.getLogger(IdentityService.class.getName());

    public static final String IDENTITY_NETWORK_SECURE = "IDENTITY_NETWORK_SECURE";
    public static final boolean IDENTITY_NETWORK_SECURE_DEFAULT = false;
    public static final String IDENTITY_NETWORK_HOST = "IDENTITY_NETWORK_HOST";
    public static final String IDENTITY_NETWORK_HOST_DEFAULT = "localhost";
    public static final String IDENTITY_NETWORK_WEBSERVER_PORT = "IDENTITY_NETWORK_WEBSERVER_PORT";
    public static final int IDENTITY_NETWORK_WEBSERVER_PORT_DEFAULT = 8080;
    public static final String IDENTITY_PROVIDER = "IDENTITY_PROVIDER";
    public static final String IDENTITY_PROVIDER_DEFAULT = "keycloak";

    // The externally visible address of this installation
    protected UriBuilder externalServerUri;
    protected IdentityProvider identityProvider;
    protected boolean devMode;

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public void init(Container container) throws Exception {
        boolean identityNetworkSecure = getBoolean(container.getConfig(), IDENTITY_NETWORK_SECURE, IDENTITY_NETWORK_SECURE_DEFAULT);
        String identityNetworkHost = getString(container.getConfig(), IDENTITY_NETWORK_HOST, IDENTITY_NETWORK_HOST_DEFAULT);
        int identityNetworkPort = getInteger(container.getConfig(), IDENTITY_NETWORK_WEBSERVER_PORT, IDENTITY_NETWORK_WEBSERVER_PORT_DEFAULT);
        devMode = container.isDevMode();

        externalServerUri = UriBuilder.fromUri("")
            .scheme(identityNetworkSecure ? "https" : "http")
            .host(identityNetworkHost);

        // Only set the port if it's not the default protocol port
        if (identityNetworkPort != 80 && identityNetworkPort != 443) {
            externalServerUri = externalServerUri.port(identityNetworkPort);
        }

        LOG.info("External system base URL: " + externalServerUri.build());

        String identityProviderType = getString(container.getConfig(), IDENTITY_PROVIDER, IDENTITY_PROVIDER_DEFAULT);
        identityProvider = createIdentityProvider(container, identityProviderType);
        identityProvider.init();
    }

    @Override
    public void start(Container container) throws Exception {
        identityProvider.start();
    }

    @Override
    public void stop(Container container) throws Exception {
        identityProvider.stop();
    }

    public UriBuilder getExternalServerUri() {
        return externalServerUri.clone();
    }

    public void secureDeployment(DeploymentInfo deploymentInfo) {
        LOG.info("Securing web deployment: " + deploymentInfo.getContextPath());
        deploymentInfo.addOuterHandlerChainWrapper(AuthOverloadHandler::new);
        deploymentInfo.setSecurityDisabled(false);
        identityProvider.secureDeployment(deploymentInfo);

        if (devMode) {
            // We need to add an undertow handler wrapper to inject CORS headers on 401/403 responses as the authentication
            // handler doesn't include headers set by deployment filters
            deploymentInfo.addOuterHandlerChainWrapper(new HandlerWrapper() {
                @Override
                public HttpHandler wrap(HttpHandler handler) {
                    return new HttpHandler() {
                        @Override
                        public void handleRequest(HttpServerExchange exchange) throws Exception {

                            if (exchange.isInIoThread()) {
                                exchange.dispatch(this);
                                return;
                            }

                            String origin = exchange.getRequestHeaders().getFirst(CorsHeaders.ORIGIN);
                            exchange.getResponseHeaders().add(HttpString.tryFromString(CorsHeaders.ACCESS_CONTROL_ALLOW_ORIGIN), origin);
                            exchange.getResponseHeaders().add(HttpString.tryFromString(CorsHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS), "true");
                            handler.handleRequest(exchange);
                        }
                    };
                }
            });
        }
    }

    /**
     * If Keycloak is enabled, support multi-tenancy.
     */
    public boolean isKeycloakEnabled() {
        return identityProvider instanceof KeycloakIdentityProvider;
    }

    /**
     * To configure the {@link IdentityProvider}, subclasses should override {@link #init(Container)}.
     */
    abstract public IdentityProvider createIdentityProvider(Container container, String type);
}