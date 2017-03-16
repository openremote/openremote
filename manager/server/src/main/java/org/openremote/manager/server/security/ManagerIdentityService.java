/*
 * Copyright 2017, OpenRemote Inc.
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
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.security.IdentityService;
import org.openremote.container.security.KeycloakResource;
import org.openremote.container.web.WebService;
import org.openremote.manager.shared.security.ClientRole;
import org.openremote.manager.shared.security.Tenant;
import org.openremote.model.Constants;

import javax.ws.rs.core.UriBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import static org.openremote.container.json.JsonUtil.convert;
import static org.openremote.model.Constants.*;

public class ManagerIdentityService extends IdentityService {

    private static final Logger LOG = Logger.getLogger(ManagerIdentityService.class.getName());

    protected boolean devMode;
    protected PersistenceService persistenceService;

    public ManagerIdentityService() {
        super(Constants.KEYCLOAK_CLIENT_ID);
    }

    @Override
    public void init(Container container) throws Exception {
        super.init(container);

        this.devMode = container.isDevMode();
        this.persistenceService = container.getService(PersistenceService.class);

        enableAuthProxy(container.getService(WebService.class));

        container.getService(WebService.class).getApiSingletons().add(
            new TenantResourceImpl(this)
        );
        container.getService(WebService.class).getApiSingletons().add(
            new UserResourceImpl(this)
        );
    }

    @Override
    protected void addClientRedirectUris(String realm, List<String> redirectUrls) {
        // Callback URL used by Manager web client authentication, any relative path to "ourselves" is fine
        String managerCallbackUrl = UriBuilder.fromUri("/").path(realm).path("*").build().toString();
        redirectUrls.add(managerCallbackUrl);

        // Callback URL used by Console web client authentication, any relative path to "ourselves" is fine
        String consoleCallbackUrl = UriBuilder.fromUri("/console/").path(realm).path("*").build().toString();
        redirectUrls.add(consoleCallbackUrl);

        // Callback URL used by AeroGear for authentication from Console
        // TODO: Do we still need this?
        redirectUrls.add("org.openremote.console://oauth2Callback");
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
        realms.sort((o1, o2) -> {
            if (o1.getRealm().equals(MASTER_REALM))
                return -1;
            if (o2.getRealm().equals(MASTER_REALM))
                return 1;
            return o1.getRealm().compareTo(o2.getRealm());
        });

        List<Tenant> tenants = new ArrayList<>();
        for (RealmRepresentation realm : realms) {
            tenants.add(convert(Container.JSON, Tenant.class, realm));
        }
        return tenants.toArray(new Tenant[tenants.size()]);
    }

    public Tenant getTenant(String bearerAuth, String realm) {
        return convert(
            Container.JSON,
            Tenant.class,
            getRealms(bearerAuth).realm(realm).toRepresentation()
        );
    }

    public boolean isActiveTenantRealm(String realm) {
        return Arrays.asList(getActiveTenantRealms()).contains(realm);
    }

    public boolean isActiveTenantRealmId(String realmId) {
        return Arrays.asList(getActiveTenantRealmIds()).contains(realmId);
    }

    public String getActiveTenantDisplayName(String realm) {
        return persistenceService.doReturningTransaction(entityManager -> {
            @SuppressWarnings("unchecked")
            List<String> result = entityManager.createNativeQuery(
                "select RA.VALUE from REALM R join REALM_ATTRIBUTE RA " +
                    "on R.ID = RA.REALM_ID and RA.NAME = 'displayName' " +
                    "and (R.ENABLED = true and (R.NOT_BEFORE is null or R.NOT_BEFORE = 0 or R.NOT_BEFORE <= extract(epoch from now()))) " +
                    "and R.NAME = :realm"
            ).setParameter("realm", realm).getResultList();
            return result.size() > 0 ? result.get(0) : null;
        });
    }

    public String getActiveTenantRealmId(String realm) {
        return persistenceService.doReturningTransaction(entityManager -> {
            @SuppressWarnings("unchecked")
            List<String> result = entityManager.createNativeQuery(
                "select R.ID from REALM R where R.NAME  = :realm " +
                    "and (R.ENABLED = true and (R.NOT_BEFORE is null or R.NOT_BEFORE = 0 or R.NOT_BEFORE <= extract(epoch from now()))) "
            ).setParameter("realm", realm).getResultList();
            return result.size() > 0 ? result.get(0) : null;
        });
    }

    public String getActiveTenantRealm(String realmId) {
        return persistenceService.doReturningTransaction(entityManager -> {
            @SuppressWarnings("unchecked")
            List<String> result = entityManager.createNativeQuery(
                "select R.NAME from REALM R where R.ID = :realmId " +
                    "and (R.ENABLED = true and (R.NOT_BEFORE is null or R.NOT_BEFORE = 0 or R.NOT_BEFORE <= extract(epoch from now())))"
            ).setParameter("realmId", realmId).getResultList();
            return result.size() > 0 ? result.get(0) : null;
        });
    }

    public String[] getActiveTenantRealmIds() {
        return persistenceService.doReturningTransaction(entityManager -> {
            @SuppressWarnings("unchecked")
            List<String> results = entityManager.createNativeQuery(
                "select R.ID from REALM R where " +
                    "(R.ENABLED = true and (R.NOT_BEFORE is null or R.NOT_BEFORE = 0 or R.NOT_BEFORE <= extract(epoch from now()))) "
            ).getResultList();
            return results.toArray(new String[results.size()]);
        });
    }

    public String[] getActiveTenantRealms() {
        return persistenceService.doReturningTransaction(entityManager -> {
            @SuppressWarnings("unchecked")
            List<String> results = entityManager.createNativeQuery(
                "select R.NAME from REALM R where " +
                    "(R.ENABLED = true and (R.NOT_BEFORE is null or R.NOT_BEFORE = 0 or R.NOT_BEFORE <= extract(epoch from now()))) "
            ).getResultList();
            return results.toArray(new String[results.size()]);
        });
    }

    public void configureRealm(RealmRepresentation realmRepresentation) {
        configureRealm(realmRepresentation, ACCESS_TOKEN_LIFESPAN_SECONDS);
    }

    public void createTenant(String bearerAuth, Tenant tenant) throws Exception {
        LOG.fine("Create tenant: " + tenant);
        RealmRepresentation realmRepresentation = convert(Container.JSON, RealmRepresentation.class, tenant);
        configureRealm(realmRepresentation);
        getRealms(bearerAuth).create(realmRepresentation);
        // TODO This is not atomic, write compensation actions
        createClientApplication(bearerAuth, realmRepresentation.getRealm());
    }

    public void createClientApplication(String bearerAuth, String realm) {
        ClientsResource clientsResource = getRealms(bearerAuth).realm(realm).clients();
        ClientRepresentation client = createClientApplication(
            realm, KEYCLOAK_CLIENT_ID, "OpenRemote", devMode
        );
        clientsResource.create(client);
        client = clientsResource.findByClientId(client.getClientId()).get(0);
        ClientResource clientResource = clientsResource.get(client.getId());
        addDefaultRoles(clientResource.roles());
    }

    public void updateTenant(String bearerAuth, String realm, Tenant tenant) throws Exception {
        LOG.fine("Update tenant: " + tenant);
        getRealms(bearerAuth).realm(realm).update(
            convert(Container.JSON, RealmRepresentation.class, tenant)
        );
    }

    public void deleteTenant(String bearerAuth, String realm) throws Exception {
        LOG.fine("Delete tenant: " + realm);
        getRealms(bearerAuth).realm(realm).remove();
    }

    public void addDefaultRoles(RolesResource rolesResource) {

        for (ClientRole clientRole : ClientRole.values()) {
            rolesResource.create(clientRole.getRepresentation());
        }

        for (ClientRole clientRole : ClientRole.values()) {
            if (clientRole.getComposites() == null)
                continue;
            List<RoleRepresentation> composites = new ArrayList<>();
            for (ClientRole composite : clientRole.getComposites()) {
                composites.add(rolesResource.get(composite.getValue()).toRepresentation());
            }
            rolesResource.get(clientRole.getValue()).addComposites(composites);
        }
    }

    public boolean isRestrictedUser(String userId) {
        UserConfiguration userConfiguration = getUserConfiguration(userId);
        return userConfiguration.isRestricted();
    }

    public void setRestrictedUser(String userId, boolean restricted) {
        UserConfiguration userConfiguration = getUserConfiguration(userId);
        userConfiguration.setRestricted(restricted);
        mergeUserConfiguration(userConfiguration);
    }

    public UserConfiguration getUserConfiguration(String userId) {
        UserConfiguration userConfiguration = persistenceService.doReturningTransaction(em -> em.find(UserConfiguration.class, userId));
        if (userConfiguration == null) {
            userConfiguration = new UserConfiguration(userId);
            userConfiguration = mergeUserConfiguration(userConfiguration);
        }
        return userConfiguration;
    }

    public UserConfiguration mergeUserConfiguration(UserConfiguration userConfiguration) {
        if (userConfiguration.getUserId() == null || userConfiguration.getUserId().length() == 0) {
            throw new IllegalArgumentException("User ID must be set on: " + userConfiguration);
        }
        return persistenceService.doReturningTransaction(em -> em.merge(userConfiguration));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            '}';
    }

}
