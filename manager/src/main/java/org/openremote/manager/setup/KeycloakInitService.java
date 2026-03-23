/*
 * Copyright 2024, OpenRemote Inc.
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

package org.openremote.manager.setup;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.security.keycloak.KeycloakResource;
import org.openremote.container.web.WebClient;
import org.openremote.container.web.WebTargetBuilder;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.util.TextUtil;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static jakarta.ws.rs.core.Response.Status.Family.REDIRECTION;
import static jakarta.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.openremote.container.security.IdentityService.OR_IDENTITY_PROVIDER;
import static org.openremote.container.security.IdentityService.OR_IDENTITY_PROVIDER_DEFAULT;
import static org.openremote.container.security.keycloak.KeycloakIdentityProvider.*;
import static org.openremote.model.util.MapAccess.getInteger;
import static org.openremote.model.util.MapAccess.getString;

/**
 * Service just to wait for keycloak availability
 *
 */
// TODO Not a great way to block startup while we wait for other services (Hystrix?)
public class KeycloakInitService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(KeycloakInitService.class.getName());

	private static final int PRIORITY = PersistenceService.PRIORITY - 10;

    @Override
    public void init(Container container) throws Exception {
        String identityProviderType = getString(container.getConfig(), OR_IDENTITY_PROVIDER, OR_IDENTITY_PROVIDER_DEFAULT);

        if (!identityProviderType.equalsIgnoreCase("keycloak")) {
            return;
        }

        waitForKeycloak(container);
    }

    @Override
    public void start(Container container) throws Exception {

    }

    @Override
    public void stop(Container container) throws Exception {

    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    public static void waitForKeycloak(Container container) {
        UriBuilder keycloakServiceUri =
            UriBuilder.fromPath("/")
                .scheme("http")
                .host(getString(container.getConfig(), OR_KEYCLOAK_HOST, OR_KEYCLOAK_HOST_DEFAULT))
                .port(getInteger(container.getConfig(), OR_KEYCLOAK_PORT, OR_KEYCLOAK_PORT_DEFAULT));

        String keycloakPath = getString(container.getConfig(), OR_KEYCLOAK_PATH, OR_KEYCLOAK_PATH_DEFAULT);

        if (!TextUtil.isNullOrEmpty(keycloakPath)) {
            keycloakServiceUri.path(keycloakPath);
        }

        ResteasyClientBuilderImpl clientBuilder = new ResteasyClientBuilderImpl()
            .connectTimeout(
                getInteger(container.getConfig(), KEYCLOAK_CONNECT_TIMEOUT, KEYCLOAK_CONNECT_TIMEOUT_DEFAULT),
                TimeUnit.MILLISECONDS
            )
            .readTimeout(
                getInteger(container.getConfig(), KEYCLOAK_REQUEST_TIMEOUT, KEYCLOAK_REQUEST_TIMEOUT_DEFAULT),
                TimeUnit.MILLISECONDS
            )
            .connectionPoolSize(
                getInteger(container.getConfig(), KEYCLOAK_CLIENT_POOL_SIZE, KEYCLOAK_CLIENT_POOL_SIZE_DEFAULT)
            );
        ResteasyClient httpClient = WebClient.registerDefaults(clientBuilder).build();

        boolean keycloakAvailable = false;
        WebTargetBuilder targetBuilder = new WebTargetBuilder(httpClient, keycloakServiceUri.build());
        ResteasyWebTarget target = targetBuilder.build();
        KeycloakResource keycloakResource = target.proxy(KeycloakResource.class);

        while (!keycloakAvailable) {
            LOG.info("Connecting to Keycloak server: " + keycloakServiceUri.build());
            try {
                pingKeycloak(keycloakResource);
                keycloakAvailable = true;
	            LOG.info("Successfully connected to Keycloak server: " + keycloakServiceUri.build());
            } catch (Exception ex) {
                LOG.info("Keycloak server not available, waiting...");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    protected static void pingKeycloak(KeycloakResource resource) throws Exception {
        Response response = null;

        try {
            response = resource.getWelcomePage();
            if (response != null &&
                (response.getStatusInfo().getFamily() == SUCCESSFUL
                    || response.getStatusInfo().getFamily() == REDIRECTION)) {
                return;
            }
            throw new Exception();
        } finally {
            if (response != null)
                response.close();
        }
    }
}
