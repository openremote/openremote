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

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmsResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.openremote.container.Container;
import org.openremote.container.security.IdentityService;
import org.openremote.container.security.KeycloakResource;
import org.openremote.container.web.WebService;
import org.openremote.manager.shared.Constants;
import org.openremote.manager.shared.security.Tenant;

import javax.ws.rs.core.UriBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import static org.openremote.container.json.JsonUtil.convert;
import static org.openremote.manager.shared.Constants.MANAGER_CLIENT_ID;
import static org.openremote.manager.shared.Constants.MANAGE_NAME;
import static org.openremote.manager.shared.Constants.MASTER_REALM;

public class ManagerIdentityService extends IdentityService {

    private static final Logger LOG = Logger.getLogger(ManagerIdentityService.class.getName());

    public static final String IDENTITY_TIMEOUT_SESSION_SECONDS = "IDENTITY_TIMEOUT_SESSION_SECONDS";
    public static final int IDENTITY_TIMEOUT_SESSION_SECONDS_DEFAULT = 10800; // 3 hours

    protected Container container;

    @Override
    public void init(Container container) throws Exception {
        this.container = container;
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
    public KeycloakResource getKeycloak(String bearerAuth) {
        return getKeycloak(bearerAuth, true);
    }

    /**
     * Pass access token from external request to Keycloak request, emulate a reverse proxy.
     */
    public RealmsResource getRealms(String bearerAuth) {
        return getRealms(bearerAuth, true);
    }

    public ClientRepresentation createManagerClient(String realm) {
        ClientRepresentation managerClient = new ClientRepresentation();

        managerClient.setClientId(MANAGER_CLIENT_ID);

        managerClient.setName(MANAGE_NAME);

        managerClient.setPublicClient(true);

        if (container.isDevMode()) {
            // We need direct access for integration tests
            LOG.warning("Allowing direct access grants for manager client, this must NOT be used in production");
            managerClient.setDirectAccessGrantsEnabled(true);
        }

        String callbackUrl = UriBuilder.fromUri("/").path(realm).path("*").build().toString();
        List<String> redirectUrls = new ArrayList<>();
        redirectUrls.add(callbackUrl);
        managerClient.setRedirectUris(redirectUrls);

        String baseUrl = UriBuilder.fromUri("/").path(realm).build().toString();
        managerClient.setBaseUrl(baseUrl);

        return managerClient;
    }

    public Tenant[] getTenants(String bearerAuth) {
        List<RealmRepresentation> realms = getRealms(bearerAuth).findAll();

        // Make sure the master tenant is always on top
        Collections.sort(realms, (o1, o2) -> {
            if (o1.getRealm().equals(MASTER_REALM))
                return -1;
            if (o2.getRealm().equals(MASTER_REALM))
                return 1;
            return o1.getRealm().compareTo(o2.getRealm());
        });

        List<Tenant> tenants = new ArrayList<>();
        for (RealmRepresentation realm : realms) {
            tenants.add(convert(container.JSON, Tenant.class, realm));
        }
        return tenants.toArray(new Tenant[tenants.size()]);
    }

    public Tenant getTenant(String bearerAuth, String realm) {
        return convert(
            container.JSON,
            Tenant.class,
            getRealms(bearerAuth).realm(realm).toRepresentation()
        );
    }

    public void createTenant(String bearerAuth, Tenant tenant) throws Exception {
        LOG.fine("Create tenant: " + tenant);
        RealmRepresentation realmRepresentation = convert(container.JSON, RealmRepresentation.class, tenant);
        configureRealm(realmRepresentation);
        getRealms(bearerAuth).create(realmRepresentation);
        // TODO This is not atomic, write compensation actions
        createClientApplication(bearerAuth, realmRepresentation.getRealm());
    }

    public void configureRealm(RealmRepresentation realmRepresentation) {
        realmRepresentation.setDisplayNameHtml(
            "<div class=\"kc-logo-text\"><span>OpenRemote: "
                + (realmRepresentation.getDisplayName().replaceAll("[^A-Za-z0-9]", ""))
                + " </span></div>"
        );
        realmRepresentation.setAccessTokenLifespan(Constants.ACCESS_TOKEN_LIFESPAN_SECONDS);
        realmRepresentation.setLoginTheme("openremote");
        realmRepresentation.setAccountTheme("openremote");
        realmRepresentation.setSsoSessionIdleTimeout(
            container.getConfigInteger(IDENTITY_TIMEOUT_SESSION_SECONDS, IDENTITY_TIMEOUT_SESSION_SECONDS_DEFAULT)
        );

        // TODO: Make SSL setup configurable
        realmRepresentation.setSslRequired(SslRequired.NONE.toString());
    }

    public void createClientApplication(String bearerAuth, String realm) {
        ClientsResource clientsResource = getRealms(bearerAuth).realm(realm).clients();
        ClientRepresentation managerClient = createManagerClient(realm);
        clientsResource.create(managerClient);
        managerClient = clientsResource.findByClientId(managerClient.getClientId()).get(0);
        ClientResource clientResource = clientsResource.get(managerClient.getId());
        addDefaultRoles(clientResource.roles());
    }

    public void updateTenant(String bearerAuth, String realm, Tenant tenant) throws Exception {
        LOG.fine("Update tenant: " + tenant);
        getRealms(bearerAuth).realm(realm).update(
            convert(container.JSON, RealmRepresentation.class, tenant)
        );
    }

    public void deleteTenant(String bearerAuth, String realm) throws Exception {
        LOG.fine("Delete tenant: " + realm);
        getRealms(bearerAuth).realm(realm).remove();
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
