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
import org.openremote.container.security.AuthForm;
import org.openremote.container.security.keycloak.KeycloakIdentityProvider;
import org.openremote.container.timer.TimerService;
import org.openremote.container.web.ClientRequestInfo;
import org.openremote.container.web.WebService;
import org.openremote.manager.apps.ConsoleAppService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.model.Constants;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetTreeModifiedEvent;
import org.openremote.model.event.shared.TenantFilter;
import org.openremote.model.query.UserQuery;
import org.openremote.model.security.*;
import org.openremote.model.util.TextUtil;

import javax.persistence.TypedQuery;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

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
        List<UserRepresentation> userRepresentations =
            getRealms(clientRequestInfo)
                .realm(realm).users().search(null, 0, Integer.MAX_VALUE);
        List<User> users = new ArrayList<>();
        for (UserRepresentation userRepresentation : userRepresentations) {
            users.add(convertUser(realm, userRepresentation));
        }
        return users.toArray(new User[users.size()]);
    }

    @Override
    public User[] getUsers(List<String> userIds) {
        return persistenceService.doReturningTransaction(em -> {
            List<User> result =
                em.createQuery("select u from User u where u.id IN :userIds", User.class)
                    .setParameter("userIds", userIds)
                    .getResultList();

            return result.toArray(new User[result.size()]);
        });
    }

    @Override
    public User[] getUsers(UserQuery userQuery) {

        LOG.fine("Building: " + userQuery);
        StringBuilder sb = new StringBuilder();
        List<Object> parameters = new ArrayList<>();
        sb.append(buildSelectString(userQuery));
        sb.append(buildFromString(userQuery));
        sb.append(buildWhereString(userQuery, parameters));
        sb.append(buildLimit(userQuery));

        return persistenceService.doReturningTransaction(entityManager -> {
            TypedQuery<User> query = entityManager.createQuery(sb.toString(), User.class);
            IntStream.range(0, parameters.size()).forEach(i -> query.setParameter(i + 1, parameters.get(i)));
            List<User> users = query.getResultList();
            return users.toArray(new User[users.size()]);
        });
    }

    protected String buildSelectString(UserQuery query) {
        return "SELECT DISTINCT(u)";
    }

    protected String buildFromString(UserQuery query) {
        StringBuilder sb = new StringBuilder();

        sb.append(" FROM User u");

        if (query.assetPredicate != null || query.pathPredicate != null) {
            sb.append(" join UserAsset ua on ua.id.userId = u.id");
        }

        return sb.toString();
    }

    protected String buildWhereString(UserQuery query, List<Object> parameters) {
        StringBuilder sb = new StringBuilder();

        sb.append(" WHERE 1=1");

        if (query.tenantPredicate != null && !TextUtil.isNullOrEmpty(query.tenantPredicate.realm)) {
            sb.append(" AND u.realm = ?").append(parameters.size() + 1);
            parameters.add(query.tenantPredicate.realm);
        }
        if (query.assetPredicate != null) {
            sb.append(" AND ua.id.assetId = ?").append(parameters.size() + 1);
            parameters.add(query.assetPredicate.id);
        }
        if (query.pathPredicate != null) {
            sb.append(" AND ?").append(parameters.size() + 1).append(" <@ get_asset_tree_path(ua.asset_id)");
            parameters.add(query.pathPredicate.path);
        }

        return sb.toString();
    }

    protected String buildLimit(UserQuery userQuery) {
        if (userQuery.limit > 0) {
            return " LIMIT " + userQuery.limit;
        }
        return "";
    }

    @Override
    public User getUser(ClientRequestInfo clientRequestInfo, String realm, String userId) {
        return convertUser(
            realm,
            getRealms(clientRequestInfo)
                .realm(realm).users().get(userId).toRepresentation()
        );
    }

    @Override
    public User getUser(String realm, String userName) {
        return persistenceService.doReturningTransaction(em -> {
            List<User> result =
                em.createQuery("select u from User u where u.realm = :realm and u.username = :username", User.class)
                    .setParameter("realm", realm)
                    .setParameter("username", userName)
                    .getResultList();
            return result.size() > 0 ? result.get(0) : null;
        });
    }

    @Override
    public void updateUser(ClientRequestInfo clientRequestInfo, String realm, String userId, User user) {
        getRealms(clientRequestInfo)
            .realm(realm).users().get(userId).update(
            convert(Container.JSON, UserRepresentation.class, user)
        );
    }

    @Override
    public void createUser(ClientRequestInfo clientRequestInfo, String realm, User user) {
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

        return roles.toArray(new Role[roles.size()]);
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

        return persistenceService.doReturningTransaction(entityManager -> {
            List<Tenant> realms = entityManager.createQuery(
                "select t from Tenant t where t.enabled = true and (t.notBefore is null or t.notBefore = 0 or to_timestamp(t.notBefore) <= now())"
            , Tenant.class).getResultList();

            // Make sure the master tenant is always on top
            realms.sort((o1, o2) -> {
                if (o1.getRealm().equals(MASTER_REALM))
                    return -1;
                if (o2.getRealm().equals(MASTER_REALM))
                    return 1;
                return o1.getRealm().compareTo(o2.getRealm());
            });

            return realms.toArray(new Tenant[realms.size()]);
        });
    }

    @Override
    public Tenant getTenant(String realm) {
        return persistenceService.doReturningTransaction(em -> {
                    List<Tenant> tenants = em.createQuery("select t from Tenant t where t.realm = :realm and t.enabled = true and (t.notBefore is null or t.notBefore = 0 or to_timestamp(t.notBefore) <= now())", Tenant.class)
                            .setParameter("realm", realm).getResultList();
                    return tenants.size() == 1 ? tenants.get(0) : null;
                }
        );
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
     * in the same realm as the assets' realm and the tenant is active.
     */
    @Override
    public boolean isTenantActiveAndAccessible(AuthContext authContext, Asset asset) {
        return isTenantActiveAndAccessible(authContext, getTenant(asset.getRealm()));
    }

    @Override
    public boolean tenantExists(String realm) {
        return persistenceService.doReturningTransaction(em -> {

            long count = em.createQuery(
                    "select count(t) from Tenant t where t.realm = :realm and t.enabled = true and (t.notBefore is null or t.notBefore = 0 or to_timestamp(t.notBefore) <= now())",
                    Long.class).setParameter("realm", realm).getSingleResult();

            return count > 0;
        });
    }

    @Override
    public boolean isRestrictedUser(String userId) {
        UserConfiguration userConfiguration = persistenceService.doReturningTransaction(em -> em.find(UserConfiguration.class, userId));
        return userConfiguration != null && userConfiguration.isRestricted();
    }

    @Override
    public boolean isUserInTenant(String userId, String realm) {
        return persistenceService.doReturningTransaction(em -> {
            User user = em.find(User.class, userId);
            return (user != null && realm.equals(user.getRealm()));
        });
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
                    MASTER_REALM, new AuthForm(ADMIN_CLI_CLIENT_ID, MASTER_REALM_ADMIN_USER, keycloakAdminPassword)
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

    protected User convertUser(String realm, UserRepresentation userRepresentation) {
        User user = convert(Container.JSON, User.class, userRepresentation);
        user.setRealm(realm);
        return user;
    }

    protected void publishModification(PersistenceEvent.Cause cause, Tenant tenant) {
        // Fire persistence event although we don't use database for Tenant CUD but call Keycloak API
        PersistenceEvent persistenceEvent = new PersistenceEvent<>(cause, tenant, new String[0], null);

        messageBrokerService.getProducerTemplate().sendBodyAndHeader(
            PersistenceEvent.PERSISTENCE_TOPIC,
            ExchangePattern.InOnly,
            persistenceEvent,
            PersistenceEvent.HEADER_ENTITY_TYPE,
            persistenceEvent.getEntity().getClass()
        );

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
