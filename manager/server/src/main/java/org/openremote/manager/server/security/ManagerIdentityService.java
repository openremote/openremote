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

import org.apache.camel.ExchangePattern;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.openremote.container.Container;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.persistence.PersistenceEvent;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.security.IdentityService;
import org.openremote.container.web.WebService;
import org.openremote.manager.server.event.EventService;
import org.openremote.manager.shared.security.ClientRole;
import org.openremote.manager.shared.security.Tenant;
import org.openremote.manager.shared.security.TenantEmailConfig;
import org.openremote.model.Constants;
import org.openremote.model.asset.AssetTreeModifiedEvent;

import javax.ws.rs.core.UriBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.openremote.model.Constants.*;
import static org.openremote.manager.server.util.JsonUtil.convert;

public class ManagerIdentityService extends IdentityService {

    private static final Logger LOG = Logger.getLogger(ManagerIdentityService.class.getName());

    protected boolean devMode;
    protected PersistenceService persistenceService;
    protected MessageBrokerService messageBrokerService;
    protected EventService eventService;

    public ManagerIdentityService() {
        super(Constants.KEYCLOAK_CLIENT_ID);
    }

    @Override
    public void init(Container container) throws Exception {
        super.init(container);

        this.devMode = container.isDevMode();
        this.persistenceService = container.getService(PersistenceService.class);
        this.messageBrokerService = container.getService(MessageBrokerService.class);
        this.eventService = container.getService(EventService.class);

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
    }

    public Tenant[] getTenants(String forwardFor, String accessToken) {
        List<RealmRepresentation> realms = getRealms(forwardFor, accessToken).findAll();

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

    public Tenant getTenantForRealm(String realm) {
        return persistenceService.doReturningTransaction(em -> {
            List<Tenant> result =
                em.createQuery("select t from Tenant t where t.realm = :realm", Tenant.class)
                    .setParameter("realm", realm).getResultList();
            return result.size() > 0 ? result.get(0) : null;
        });
    }

    public Tenant getTenant(String realmId) {
        return persistenceService.doReturningTransaction(em -> em.find(Tenant.class, realmId));
    }

    public boolean isActiveTenant(String realmId) {
        return persistenceService.doReturningTransaction(em -> {
            Tenant tenant = em.find(Tenant.class, realmId);
            return tenant != null && tenant.isActive();
        });
    }

    public String[] getActiveTenantIds() {
        return persistenceService.doReturningTransaction(entityManager -> {
            @SuppressWarnings("unchecked")
            List<String> results = entityManager.createQuery(
                "select t.id from Tenant t where " +
                    "t.enabled = true and (t.notBefore is null or t.notBefore = 0 or to_timestamp(t.notBefore) <= now())"
            ).getResultList();
            return results.toArray(new String[results.size()]);
        });
    }

    public void configureRealm(RealmRepresentation realmRepresentation) {
        configureRealm(realmRepresentation, ACCESS_TOKEN_LIFESPAN_SECONDS);
    }

    public void createTenant(String forwardFor, String accessToken, Tenant tenant) throws Exception {
        createTenant(forwardFor, accessToken, tenant, null);
    }

    public void createTenant(String forwardFor, String accessToken, Tenant tenant, TenantEmailConfig emailConfig) throws Exception {
        LOG.fine("Create tenant: " + tenant);
        RealmRepresentation realmRepresentation = convert(Container.JSON, RealmRepresentation.class, tenant);
        if (emailConfig != null) {
            realmRepresentation.setSmtpServer(emailConfig.asMap());
        }
        configureRealm(realmRepresentation);
        getRealms(forwardFor, accessToken).create(realmRepresentation);
        // TODO This is not atomic, write compensation actions
        createClientApplication(forwardFor, accessToken, realmRepresentation.getRealm());
        publishModification(PersistenceEvent.Cause.INSERT, tenant);
    }

    public void createClientApplication(String forwardFor, String accessToken, String realm) {
        ClientsResource clientsResource = getRealms(forwardFor, accessToken).realm(realm).clients();
        ClientRepresentation client = createClientApplication(
            realm, KEYCLOAK_CLIENT_ID, "OpenRemote", devMode
        );
        clientsResource.create(client);
        client = clientsResource.findByClientId(client.getClientId()).get(0);
        ClientResource clientResource = clientsResource.get(client.getId());
        addDefaultRoles(clientResource.roles());
    }

    public void updateTenant(String forwardFor, String accessToken, String realm, Tenant tenant) throws Exception {
        LOG.fine("Update tenant: " + tenant);
        getRealms(forwardFor, accessToken).realm(realm).update(
            convert(Container.JSON, RealmRepresentation.class, tenant)
        );
        publishModification(PersistenceEvent.Cause.UPDATE, tenant);
    }

    public void deleteTenant(String forwardFor, String accessToken, String realm) throws Exception {
        Tenant tenant = getTenantForRealm(realm);
        LOG.fine("Delete tenant: " + realm);
        getRealms(forwardFor, accessToken).realm(realm).remove();
        publishModification(PersistenceEvent.Cause.DELETE, tenant);
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

    /**
     * Use PERSISTENCE_TOPIC and persistence event
     */
    protected void publishModification(PersistenceEvent.Cause cause, Tenant tenant) {
        PersistenceEvent persistenceEvent = new PersistenceEvent<>(cause, tenant, new String[0], null);

        messageBrokerService.getProducerTemplate().sendBodyAndHeader(
            PersistenceEvent.PERSISTENCE_TOPIC,
            ExchangePattern.InOnly,
            persistenceEvent,
            PersistenceEvent.HEADER_ENTITY_TYPE,
            persistenceEvent.getEntity().getClass()
        );

        eventService.publishEvent(
            new AssetTreeModifiedEvent(tenant.getId(), null)
        );
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            '}';
    }

}
