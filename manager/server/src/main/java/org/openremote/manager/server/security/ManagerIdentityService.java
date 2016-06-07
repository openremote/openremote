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
package org.openremote.manager.server.security;

import org.keycloak.admin.client.resource.RealmsResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.openremote.container.Container;
import org.openremote.container.security.IdentityService;
import org.openremote.container.security.KeycloakResource;
import org.openremote.container.web.WebService;
import org.openremote.manager.shared.Constants;
import org.openremote.manager.shared.http.RequestParams;

import javax.ws.rs.core.UriBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.openremote.manager.shared.Constants.MANAGER_CLIENT_ID;
import static org.openremote.manager.shared.Constants.MANAGE_NAME;
import static org.openremote.manager.shared.Constants.MASTER_REALM;

public class ManagerIdentityService extends IdentityService {

    @Override
    public void init(Container container) throws Exception {
        setClientId(Constants.MANAGER_CLIENT_ID);
        super.init(container);
        setKeycloakReverseProxy(true);
    }

    @Override
    public void configure(Container container) throws Exception {
        super.configure(container);

        container.getService(WebService.class).getApiSingletons().add(
            new TenantResourceImpl(this)
        );
        container.getService(WebService.class).getApiSingletons().add(
            new UserResourceImpl(this)
        );
    }

    /**
     * Pass access token from external request to Keycloak request, emulate a reverse proxy.
     */
    public KeycloakResource getKeycloak(RequestParams requestParams) {
        return getKeycloak(requestParams.getBearerAuth(), true);
    }

    /**
     * Pass access token from external request to Keycloak request, emulate a reverse proxy.
     */
    public RealmsResource getRealms(RequestParams requestParams) {
        return getRealms(requestParams.getBearerAuth(), true);
    }

    public ClientRepresentation createManagerClient() {
        ClientRepresentation managerClient = new ClientRepresentation();
        managerClient.setClientId(MANAGER_CLIENT_ID);
        managerClient.setName(MANAGE_NAME);
        managerClient.setPublicClient(true);
        String callbackUrl = UriBuilder.fromUri("/").path(MASTER_REALM).path("*").build().toString();

        List<String> redirectUrls = new ArrayList<>();
        redirectUrls.add(callbackUrl);
        managerClient.setRedirectUris(redirectUrls);

        String baseUrl = UriBuilder.fromUri("/").path(MASTER_REALM).build().toString();
        managerClient.setBaseUrl(baseUrl);

        return managerClient;
    }

    public void addDefaultRoles(RolesResource rolesResource) {
        rolesResource.create(new RoleRepresentation("read", "Read all data", false));
        rolesResource.create(new RoleRepresentation("write", "Write all data", false));

        rolesResource.create(new RoleRepresentation("read:map", "View map", false));
        rolesResource.create(new RoleRepresentation("read:assets", "Read context broker assets", false));
        rolesResource.get("read").addComposites(Arrays.asList(
            rolesResource.get("read:map").toRepresentation(),
            rolesResource.get("read:assets").toRepresentation()
        ));

        rolesResource.create(new RoleRepresentation("write:assets", "Write context broker assets", false));
        rolesResource.get("write").addComposites(Arrays.asList(
            rolesResource.get("write:assets").toRepresentation()
        ));
    }
}
