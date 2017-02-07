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
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.openremote.container.Container;
import org.openremote.container.security.IdentityService;
import org.openremote.container.security.KeycloakResource;
import org.openremote.container.web.WebService;
import org.openremote.manager.shared.Constants;
import org.openremote.manager.shared.security.Tenant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import static org.openremote.container.json.JsonUtil.convert;
import static org.openremote.manager.shared.Constants.*;

public class ManagerIdentityService extends IdentityService {

    private static final Logger LOG = Logger.getLogger(ManagerIdentityService.class.getName());

    protected Container container;

    @Override
    public void init(Container container) throws Exception {
        this.container = container;
        setClientId(Constants.APP_CLIENT_ID);
        super.init(container);
        enableAuthProxy(container.getService(WebService.class));

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

    public void configureRealm(RealmRepresentation realmRepresentation) {
        configureRealm(realmRepresentation, ACCESS_TOKEN_LIFESPAN_SECONDS);
    }

    public void createTenant(String bearerAuth, Tenant tenant) throws Exception {
        LOG.fine("Create tenant: " + tenant);
        RealmRepresentation realmRepresentation = convert(container.JSON, RealmRepresentation.class, tenant);
        configureRealm(realmRepresentation);
        getRealms(bearerAuth).create(realmRepresentation);
        // TODO This is not atomic, write compensation actions
        createClientApplication(bearerAuth, realmRepresentation.getRealm());
    }

    public void createClientApplication(String bearerAuth, String realm) {
        ClientsResource clientsResource = getRealms(bearerAuth).realm(realm).clients();
        ClientRepresentation client = createClientApplication(
            realm, APP_CLIENT_ID, APP_NAME, container.isDevMode()
        );
        clientsResource.create(client);
        client = clientsResource.findByClientId(client.getClientId()).get(0);
        ClientResource clientResource = clientsResource.get(client.getId());
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
