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
package org.openremote.manager.security;

import org.apache.camel.ExchangePattern;
import org.keycloak.TokenVerifier;
import org.keycloak.admin.client.resource.*;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.*;
import org.openremote.container.Container;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.persistence.PersistenceEvent;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.security.AuthContext;
import org.openremote.container.security.PasswordAuthForm;
import org.openremote.container.security.keycloak.KeycloakIdentityProvider;
import org.openremote.container.timer.TimerService;
import org.openremote.container.web.ClientRequestInfo;
import org.openremote.container.web.WebService;
import org.openremote.manager.apps.ConsoleAppService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.model.Constants;
import org.openremote.model.asset.AssetTreeModifiedEvent;
import org.openremote.model.event.shared.TenantFilter;
import org.openremote.model.query.UserQuery;
import org.openremote.model.query.filter.TenantPredicate;
import org.openremote.model.security.*;
import org.openremote.model.util.TextUtil;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.container.util.JsonUtil.convert;
import static org.openremote.container.web.WebClient.getTarget;
import static org.openremote.manager.setup.AbstractKeycloakSetup.*;
import static org.openremote.model.Constants.*;

public class ManagerKeycloakIdentityProvider extends KeycloakIdentityProvider implements ManagerIdentityProvider {

    private static final Logger LOG = Logger.getLogger(ManagerKeycloakIdentityProvider.class.getName());

    final boolean devMode;
    final protected PersistenceService persistenceService;
    final protected TimerService timerService;
    final protected MessageBrokerService messageBrokerService;
    final protected ClientEventService clientEventService;
    final protected ConsoleAppService consoleAppService;
    final protected String keycloakAdminPassword;

    public ManagerKeycloakIdentityProvider(UriBuilder externalServerUri, Container container) {
        super(KEYCLOAK_CLIENT_ID, externalServerUri, container);

        this.keycloakAdminPassword = container.getConfig().getOrDefault(SETUP_ADMIN_PASSWORD, SETUP_ADMIN_PASSWORD_DEFAULT);
        this.devMode = container.isDevMode();
        this.timerService = container.getService(TimerService.class);
        this.persistenceService = container.getService(PersistenceService.class);
        this.messageBrokerService = container.getService(MessageBrokerService.class);
        this.clientEventService = container.getService(ClientEventService.class);
        this.consoleAppService = container.getService(ConsoleAppService.class);

        enableAuthProxy(container.getService(WebService.class));
    }

    @Override
    protected void addClientRedirectUris(String client, List<String> redirectUrls) {
        if (devMode) {
            // Allow any redirect URIs in dev mode
            redirectUrls.add("*");
        } else {
            // Callback URL used by Manager web client authentication, any relative path to "ourselves" is fine
            String realmManagerCallbackUrl = UriBuilder.fromUri("/").path(client).path("*").build().toString();
            redirectUrls.add(realmManagerCallbackUrl);

            // Callback URL used by Console web client authentication, any relative path to "ourselves" is fine
            String consoleCallbackUrl = UriBuilder.fromUri("/console/").path(client).path("*").build().toString();
            redirectUrls.add(consoleCallbackUrl);
        }
    }

    @Override
    public User[] getUsers(ClientRequestInfo clientRequestInfo, String realm) {
        return getUsers(new UserQuery().tenant(new TenantPredicate(realm)));
    }

    @Override
    public User[] getUsers(List<String> userIds) {
        return getUsers(new UserQuery().ids(userIds.toArray(new String[0])));
    }

    @Override
    public User[] getUsers(UserQuery userQuery) {
        return ManagerIdentityProvider.getUsersFromDb(persistenceService, userQuery);
    }

    @Override
    public User getUser(ClientRequestInfo clientRequestInfo, String realm, String userId) {
        return ManagerIdentityProvider.getUserByIdFromDb(persistenceService, realm, userId);
    }

    @Override
    public User getUser(String realm, String username) {
        return ManagerIdentityProvider.getUserByUsernameFromDb(persistenceService, realm, username);
    }

    @Override
    public void updateUser(ClientRequestInfo clientRequestInfo, String realm, String userId, User user) {
        getRealms(clientRequestInfo)
            .realm(realm).users().get(userId).update(
            convert(Container.JSON, UserRepresentation.class, user)
        );
    }

    @Override
    public void createUser(ClientRequestInfo clientRequestInfo, String realm, User user, String password) {
        Response response = getRealms(clientRequestInfo)
            .realm(realm).users().create(
                convert(Container.JSON, UserRepresentation.class, user)
            );
        if (!response.getStatusInfo().equals(Response.Status.CREATED)) {
            throw new WebApplicationException(
                Response.status(response.getStatus())
                    .entity(response.getEntity())
                    .build()
            );
        } else {
            response.close();
        }
    }

    @Override
    public void deleteUser(ClientRequestInfo clientRequestInfo, String realm, String userId) {
        Response response = getRealms(clientRequestInfo)
            .realm(realm).users().delete(userId);
        if (!response.getStatusInfo().equals(Response.Status.NO_CONTENT)) {
            throw new WebApplicationException(
                Response.status(response.getStatus())
                    .entity(response.getEntity())
                    .build()
            );
        } else {
            response.close();
        }
    }

    @Override
    public void resetPassword(ClientRequestInfo clientRequestInfo, String realm, String userId, Credential credential) {
        getRealms(clientRequestInfo)
            .realm(realm).users().get(userId).resetPassword(
            convert(Container.JSON, CredentialRepresentation.class, credential)
        );
    }

    @Override
    public Role[] getRoles(ClientRequestInfo clientRequestInfo, String realm, String userId) {
        RoleMappingResource roleMappingResource = getRealms(clientRequestInfo).realm(realm).users().get(userId).roles();
        ClientsResource clientsResource = getRealms(clientRequestInfo).realm(realm).clients();

        String clientId = clientsResource.findByClientId(KEYCLOAK_CLIENT_ID).get(0).getId();
        RolesResource rolesResource = clientsResource.get(clientId).roles();

        List<RoleRepresentation> allRoles = rolesResource.list();
        List<RoleRepresentation> effectiveRoles = roleMappingResource.clientLevel(clientId).listEffective();

        List<Role> roles = new ArrayList<>();
        for (RoleRepresentation roleRepresentation : allRoles) {
            boolean isAssigned = false;

            for (RoleRepresentation effectiveRole : effectiveRoles) {
                if (effectiveRole.getId().equals(roleRepresentation.getId()))
                    isAssigned = true;
            }

            roles.add(new Role(
                roleRepresentation.getId(),
                roleRepresentation.getName(),
                roleRepresentation.isComposite(),
                isAssigned
            ));
        }

        return roles.toArray(new Role[0]);
    }

    @Override
    public void updateRoles(ClientRequestInfo clientRequestInfo, String realm, String userId, Role[] roles) {
        RoleMappingResource roleMappingResource = getRealms(clientRequestInfo).realm(realm).users().get(userId).roles();
        ClientsResource clientsResource = getRealms(clientRequestInfo).realm(realm).clients();

        String clientId = clientsResource.findByClientId(KEYCLOAK_CLIENT_ID).get(0).getId();

        List<RoleRepresentation> rolesToAdd = new ArrayList<>();
        List<RoleRepresentation> rolesToRemove = new ArrayList<>();

        for (Role role : roles) {
            RoleRepresentation roleRepresentation = new RoleRepresentation();
            roleRepresentation.setId(role.getId());
            roleRepresentation.setName(role.getName());
            if (role.isAssigned()) {
                rolesToAdd.add(roleRepresentation);
            } else {
                rolesToRemove.add(roleRepresentation);
            }
        }

        roleMappingResource.clientLevel(clientId).add(rolesToAdd);
        roleMappingResource.clientLevel(clientId).remove(rolesToRemove);
    }

    @Override
    public boolean isMasterRealmAdmin(ClientRequestInfo clientRequestInfo, String userId) {
        List<UserRepresentation> adminUsers =
            // TODO Why null here in getRealms()?
            getRealms(null, clientRequestInfo.getAccessToken()).realm(MASTER_REALM)
                .users().search(MASTER_REALM_ADMIN_USER, null, null);
        if (adminUsers.size() == 0) {
            throw new IllegalStateException("Can't load master realm admin user");
        } else if (adminUsers.size() > 1) {
            throw new IllegalStateException("Several master realm admin users, this should not be possible.");
        }
        return adminUsers.get(0).getId().equals(userId);
    }

    @Override
    public Tenant[] getTenants() {
        return ManagerIdentityProvider.getTenantsFromDb(persistenceService);
    }

    @Override
    public Tenant getTenant(String realm) {
        return ManagerIdentityProvider.getTenantFromDb(persistenceService, realm);
    }

    @Override
    public void updateTenant(ClientRequestInfo clientRequestInfo, String realm, Tenant tenant) {
        LOG.fine("Update tenant: " + tenant);

        getRealms(new ClientRequestInfo(clientRequestInfo.getRemoteAddress(), getAdminAccessToken(clientRequestInfo))).realm(realm).update(
            convert(Container.JSON, RealmRepresentation.class, tenant)
        );
        publishModification(PersistenceEvent.Cause.UPDATE, tenant);
    }

    @Override
    public void createTenant(ClientRequestInfo clientRequestInfo, Tenant tenant) {
        createTenant(clientRequestInfo, tenant, null);
    }

    @Override
    public void createTenant(ClientRequestInfo clientRequestInfo, Tenant tenant, TenantEmailConfig emailConfig) {
        LOG.fine("Create tenant: " + tenant);
        RealmRepresentation realmRepresentation = convert(Container.JSON, RealmRepresentation.class, tenant);
        configureRealm(realmRepresentation, emailConfig);
        clientRequestInfo = new ClientRequestInfo(clientRequestInfo.getRemoteAddress(), getAdminAccessToken(clientRequestInfo));
        // TODO This is not atomic, write compensation actions
        getRealms(clientRequestInfo).create(realmRepresentation);
        createClientApplication(clientRequestInfo, realmRepresentation.getRealm());

        publishModification(PersistenceEvent.Cause.CREATE, tenant);
    }

    @Override
    public void deleteTenant(ClientRequestInfo clientRequestInfo, String realm) {
        Tenant tenant = getTenant(realm);
        if (tenant != null) {
            LOG.fine("Delete tenant: " + realm);
            getRealms(new ClientRequestInfo(clientRequestInfo.getRemoteAddress(), getAdminAccessToken(clientRequestInfo))).realm(realm).remove();
            publishModification(PersistenceEvent.Cause.DELETE, tenant);
        }
    }

    /**
     * @return <code>true</code> if the user is the superuser (admin) or if the user is authenticated
     * in the same realm as the tenant and the tenant is active.
     */
    @Override
    public boolean isTenantActiveAndAccessible(AuthContext authContext, Tenant tenant) {
        return tenant != null && (authContext.isSuperUser()
            || (tenant.isActive(timerService.getCurrentTimeMillis()) && authContext.isRealmAccessibleByUser(tenant.getRealm())));
    }

    /**
     * @return <code>true</code> if the user is the superuser (admin) or if the user is authenticated
     * in the same tenant and the tenant is active.
     */
    @Override
    public boolean isTenantActiveAndAccessible(AuthContext authContext, String realm) {
        return isTenantActiveAndAccessible(authContext, getTenant(realm));
    }

    @Override
    public boolean tenantExists(String realm) {
        return ManagerIdentityProvider.tenantExistsFromDb(persistenceService, realm);
    }

    @Override
    public boolean isRestrictedUser(String userId) {
        UserConfiguration userConfiguration = persistenceService.doReturningTransaction(em -> em.find(UserConfiguration.class, userId));
        return userConfiguration != null && userConfiguration.isRestricted();
    }

    @Override
    public boolean isUserInTenant(String userId, String realm) {
        return ManagerIdentityProvider.userInTenantFromDb(persistenceService, userId, realm);
    }

    @Override
    public boolean canSubscribeWith(AuthContext auth, TenantFilter filter, ClientRole... requiredRoles) {
        // Superuser can always subscribe
        if (auth.isSuperUser())
            return true;

        // Restricted users get nothing
        if (isRestrictedUser(auth.getUserId()))
            return false;

        // User must have role
        if (requiredRoles != null) {
            for (ClientRole requiredRole : requiredRoles) {
                if (!auth.hasResourceRole(requiredRole.getValue(), Constants.KEYCLOAK_CLIENT_ID)) {
                    return false;
                }
            }
        }

        // Ensure filter matches authenticated realm
        if (filter != null) {
            String authenticatedRealm = auth.getAuthenticatedRealm();

            if (TextUtil.isNullOrEmpty(authenticatedRealm))
                return false;
            if (authenticatedRealm.equals(filter.getRealm()))
                return true;
        }

        return false;

    }

    public void configureRealm(RealmRepresentation realmRepresentation, TenantEmailConfig emailConfig) {
        configureRealm(realmRepresentation, Constants.ACCESS_TOKEN_LIFESPAN_SECONDS);
        if (emailConfig != null)
            realmRepresentation.setSmtpServer(emailConfig.asMap());
    }

    public void createClientApplication(ClientRequestInfo clientRequestInfo, String realm) {
        ClientRepresentation client = createDefaultClientRepresentation(
            realm, KEYCLOAK_CLIENT_ID, "OpenRemote", devMode
        );
        createClientApplication(clientRequestInfo, realm, client);
    }

    public void createClientApplication(ClientRequestInfo clientRequestInfo, String realm, ClientRepresentation client) {
        ClientsResource clientsResource = getRealms(clientRequestInfo).realm(realm).clients();
        clientsResource.create(client);
        client = clientsResource.findByClientId(client.getClientId()).get(0);
        ClientResource clientResource = clientsResource.get(client.getId());
        addDefaultRoles(clientResource.roles());
    }

    /**
     * Keycloak only allows realm CRUD using the {realm}-realm client or the admin-cli client so we need to ensure we
     * have a token for one of these realms; if we are creating a realm then that means using the admin-cli
     */
    public String getAdminAccessToken(ClientRequestInfo clientRequestInfo) {
        try {
            AccessToken token = clientRequestInfo != null ? TokenVerifier.create(clientRequestInfo.getAccessToken(), AccessToken.class).getToken() : null;

            if (token == null || !token.getIssuedFor().equals(ADMIN_CLI_CLIENT_ID)) {
                return getKeycloak().getAccessToken(
                    MASTER_REALM, new PasswordAuthForm(ADMIN_CLI_CLIENT_ID, MASTER_REALM_ADMIN_USER, keycloakAdminPassword)
                ).getToken();
            }
            return clientRequestInfo.getAccessToken();
        } catch (VerificationException e) {
            LOG.log(Level.WARNING, "Failed to parse access token", e);
            return clientRequestInfo.getAccessToken();
        }
    }

    protected ClientRepresentation createDefaultClientRepresentation(String realm, String clientId, String appName, boolean devMode) {
        ClientRepresentation client = new ClientRepresentation();

        client.setClientId(clientId);
        client.setName(appName);
        client.setPublicClient(true);

        if (devMode) {
            // We need direct access for integration tests
            LOG.info("### Allowing direct access grants for client app '" + appName + "', this must NOT be used in production! ###");
            client.setDirectAccessGrantsEnabled(true);

            // Allow any web origin (this will add CORS headers to token requests etc.)
            client.setWebOrigins(Collections.singletonList("*"));
            client.setRedirectUris(Collections.singletonList("*"));
        } else {
            List<String> redirectUris = new ArrayList<>();
            try {
                for (String consoleName : consoleAppService.getInstalled()) {
                    addClientRedirectUris(consoleName, redirectUris);
                }
            } catch (Exception exception) {
                LOG.log(Level.WARNING, exception.getMessage(), exception);
                addClientRedirectUris(realm, redirectUris);
            }

            client.setRedirectUris(redirectUris);
        }

        return client;
    }

    protected void addDefaultRoles(RolesResource rolesResource) {

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

    public static User convertUser(String realm, UserRepresentation userRepresentation) {
        User user = convert(Container.JSON, User.class, userRepresentation);
        user.setRealm(realm);
        return user;
    }

    protected void publishModification(PersistenceEvent.Cause cause, Tenant tenant) {
        // Fire persistence event although we don't use database for Tenant CUD but call Keycloak API
        PersistenceEvent persistenceEvent = new PersistenceEvent<>(cause, tenant, new String[0], null);

        if (messageBrokerService.getProducerTemplate() != null) {
            messageBrokerService.getProducerTemplate().sendBodyAndHeader(
                PersistenceEvent.PERSISTENCE_TOPIC,
                ExchangePattern.InOnly,
                persistenceEvent,
                PersistenceEvent.HEADER_ENTITY_TYPE,
                persistenceEvent.getEntity().getClass()
            );
        }

        clientEventService.publishEvent(
            new AssetTreeModifiedEvent(timerService.getCurrentTimeMillis(), tenant.getRealm(), null)
        );
    }

    public String addLDAPConfiguration(ClientRequestInfo clientRequestInfo, String realm, ComponentRepresentation componentRepresentation) {
        RealmResource realmResource = getRealms(clientRequestInfo)
            .realm(realm);
        Response response = realmResource.components().add(componentRepresentation);

        if (!response.getStatusInfo().equals(Response.Status.CREATED)) {
            throw new WebApplicationException(
                Response.status(response.getStatus())
                    .entity(response.getEntity())
                    .build()
            );
        } else {
            response.close();

            componentRepresentation = realmResource.components()
                .query(componentRepresentation.getParentId(),
                    componentRepresentation.getProviderType(),
                    componentRepresentation.getName()).get(0);
            response = syncUsers(clientRequestInfo, componentRepresentation.getId(), realm, "triggerFullSync");

            if (!response.getStatusInfo().equals(Response.Status.OK)) {
                throw new WebApplicationException(
                    Response.status(response.getStatus())
                        .entity(response.getEntity())
                        .build()
                );
            } else {
                response.close();
            }
        }
        return componentRepresentation.getId();
    }

    public String addLDAPMapper(ClientRequestInfo clientRequestInfo, String realm, ComponentRepresentation componentRepresentation) {
        RealmResource realmResource = getRealms(clientRequestInfo)
            .realm(realm);
        Response response = realmResource.components().add(componentRepresentation);

        if (!response.getStatusInfo().equals(Response.Status.CREATED)) {
            throw new WebApplicationException(
                Response.status(response.getStatus())
                    .entity(response.getEntity())
                    .build()
            );
        } else {
            response.close();

            componentRepresentation = realmResource.components()
                .query(componentRepresentation.getParentId(),
                    componentRepresentation.getProviderType(),
                    componentRepresentation.getName()).get(0);
            realmResource.userStorage().syncMapperData(componentRepresentation.getParentId(), componentRepresentation.getId(), "fedToKeycloak");
        }
        return componentRepresentation.getId();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{}";
    }
}
