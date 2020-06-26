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
import org.keycloak.admin.client.resource.*;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.enums.SslRequired;
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
import org.openremote.manager.concurrent.ManagerExecutorService;
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
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.openremote.container.security.IdentityService.IDENTITY_NETWORK_HOST;
import static org.openremote.container.security.IdentityService.IDENTITY_NETWORK_HOST_DEFAULT;
import static org.openremote.container.util.JsonUtil.convert;
import static org.openremote.container.util.MapAccess.getBoolean;
import static org.openremote.container.util.MapAccess.getString;
import static org.openremote.container.web.WebService.WEBSERVER_ALLOWED_ORIGINS;
import static org.openremote.container.web.WebService.WEBSERVER_ALLOWED_ORIGINS_DEFAULT;
import static org.openremote.manager.setup.AbstractKeycloakSetup.SETUP_EMAIL_FROM_KEYCLOAK;
import static org.openremote.manager.setup.AbstractKeycloakSetup.SETUP_EMAIL_FROM_KEYCLOAK_DEFAULT;
import static org.openremote.model.Constants.*;

/**
 * All keycloak interaction is done through the admin-cli client; security is implemented downstream of here; anything
 * in the same process as this service has privileged access to keycloak.
 */
public class ManagerKeycloakIdentityProvider extends KeycloakIdentityProvider implements ManagerIdentityProvider {

    private static final Logger LOG = Logger.getLogger(ManagerKeycloakIdentityProvider.class.getName());
    public static final String REALM_KEYCLOAK_THEME_SUFFIX = "_REALM_KEYCLOAK_THEME";
    public static final String DEFAULT_REALM_KEYCLOAK_THEME = "DEFAULT_REALM_KEYCLOAK_THEME";
    public static final String DEFAULT_REALM_KEYCLOAK_THEME_DEFAULT = "openremote";

    final boolean devMode;
    final protected PersistenceService persistenceService;
    final protected TimerService timerService;
    final protected MessageBrokerService messageBrokerService;
    final protected ClientEventService clientEventService;
    final protected ConsoleAppService consoleAppService;
    final protected String keycloakAdminPassword;
    final protected Container container;

    public ManagerKeycloakIdentityProvider(UriBuilder externalServerUri, Container container) {
        super(KEYCLOAK_CLIENT_ID, externalServerUri, container.getService(ManagerExecutorService.class), container);

        this.container = container;
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
    public User[] getUsers(String realm) {
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
    public User getUser(String realm, String userId) {
        return ManagerIdentityProvider.getUserByIdFromDb(persistenceService, realm, userId);
    }

    @Override
    public User getUserByUsername(String realm, String username) {
        return ManagerIdentityProvider.getUserByUsernameFromDb(persistenceService, realm, username);
    }

    @Override
    public void updateUser(String realm, User user) {
        // User only has a subset of user representation so overlay on actual user representation
        UserResource userResource = getRealms().realm(realm).users().get(user.getId());
        UserRepresentation userRepresentation = userResource.toRepresentation();
        userRepresentation.setUsername(user.getUsername());
        userRepresentation.setFirstName(user.getFirstName());
        userRepresentation.setLastName(user.getLastName());
        userRepresentation.setEmail(user.getEmail());
        userRepresentation.setEnabled(user.getEnabled());
        userResource.update(userRepresentation);
    }

    @Override
    public User createUser(String realm, User user, String password) {
        RealmResource realmResource = getRealms().realm(realm);
        Response response = realmResource.users().create(
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
            user = getUserByUsername(realm, user.getUsername().toLowerCase());//Username is stored in lowercase in Keycloak
            if (user != null && !TextUtil.isNullOrEmpty(password)) {
                CredentialRepresentation credentials = new CredentialRepresentation();
                credentials.setType("password");
                credentials.setValue(password);
                credentials.setTemporary(false);
                realmResource.users().get(user.getId()).resetPassword(credentials);
                LOG.info("Created user '" + user.getUsername() + "' with password '" + password + "'");
            }
        }

        return user;
    }

    @Override
    public void deleteUser(String realm, String userId) {
        Response response = getRealms()
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
    public void resetPassword(String realm, String userId, Credential credential) {
        getRealms()
            .realm(realm).users().get(userId).resetPassword(
            convert(Container.JSON, CredentialRepresentation.class, credential)
        );
    }

    @Override
    public Role[] getRoles(String realm, String userId) {
        RealmResource realmResource = getRealms().realm(realm);
        RoleMappingResource roleMappingResource = realmResource.users().get(userId).roles();
        ClientsResource clientsResource = realmResource.clients();

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
    public void updateRoles(String realm, String userId, ClientRole[] roles) {
        RealmResource realmResource = getRealms().realm(realm);
        UserRepresentation user = realmResource.users().get(userId).toRepresentation();

        if (user == null) {
            throw new IllegalStateException("Multiple users with the same username found");
        }

        RoleMappingResource roleMappingResource = realmResource.users().get(user.getId()).roles();
        ClientRepresentation client = getClient(realm, KEYCLOAK_CLIENT_ID);

        // Get all role mappings for user on this client and remove any no longer in the roles
        List<RoleRepresentation> clientMappedRoles = roleMappingResource.clientLevel(client.getId()).listAll();
        List<RoleRepresentation> availableRoles = roleMappingResource.clientLevel(client.getId()).listAvailable();

        // Get newly defined roles
        List<RoleRepresentation> addRoles = Arrays.stream(roles)
            .filter(cr -> clientMappedRoles.stream().noneMatch(r -> r.getName().equals(cr.getValue())))
            .map(cr -> availableRoles.stream().filter(r -> r.getName().equals(cr.getValue())).findFirst().orElse(null))
            .collect(Collectors.toList());

        // Remove obsolete roles
        List<RoleRepresentation> removeRoles = clientMappedRoles.stream()
            .filter(r -> Arrays.stream(roles).noneMatch(cr -> cr.getValue().equals(r.getName())))
            .collect(Collectors.toList());

        if (!removeRoles.isEmpty()) {
            roleMappingResource.clientLevel(client.getId()).remove(removeRoles);
        }
        if (!addRoles.isEmpty()) {
            roleMappingResource.clientLevel(client.getId()).add(addRoles);
        }
    }

    @Override
    public boolean isMasterRealmAdmin(String userId) {
        List<UserRepresentation> adminUsers = getRealms()
            .realm(MASTER_REALM)
            .users()
            .search(MASTER_REALM_ADMIN_USER, null, null);

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
        RealmRepresentation realmRepresentation = getRealms().realm(realm).toRepresentation();
        return convert(Container.JSON, Tenant.class, realmRepresentation);
    }

    @Override
    public void updateTenant(Tenant tenant) {
        LOG.fine("Update tenant: " + tenant);
        RealmsResource realmsResource = getRealms();

        // Find existing realm by ID as realm name could have been changed
        RealmRepresentation existing = realmsResource.findAll().stream().filter(r -> r.getId().equals(tenant.getId())).findFirst().orElse(null);
        if (existing == null) {
            throw new IllegalStateException("Tenant must already exist, ID does not match an existing tenant");
        }

        String realm = existing.getRealm();

        // Tenant only has a subset of realm representation so overlay on actual realm representation
        existing.setRealm(tenant.getRealm());
        existing.setDisplayName(tenant.getDisplayName());
        existing.setAccountTheme(tenant.getAccountTheme());
        existing.setAdminTheme(tenant.getAdminTheme());
        existing.setEmailTheme(tenant.getEmailTheme());
        existing.setLoginTheme(tenant.getLoginTheme());
        existing.setRememberMe(tenant.getRememberMe());
        existing.setEnabled(tenant.getEnabled());
        existing.setDuplicateEmailsAllowed(tenant.getDuplicateEmailsAllowed());
        existing.setResetPasswordAllowed(tenant.getResetPasswordAllowed());
        existing.setNotBefore(tenant.getNotBefore() != null ? tenant.getNotBefore().intValue() : null);
        configureRealm(existing);

        realmsResource.realm(realm).update(existing);
        publishModification(PersistenceEvent.Cause.UPDATE, tenant);
    }

    @Override
    public Tenant createTenant(Tenant tenant) {
        LOG.fine("Create tenant: " + tenant);
        RealmsResource realmsResource = getRealms();
        RealmRepresentation realmRepresentation = convert(Container.JSON, RealmRepresentation.class, tenant);
        realmsResource.create(realmRepresentation);
        RealmResource realmResource = realmsResource.realm(tenant.getRealm());

        realmRepresentation = realmResource.toRepresentation();
        // Need a committed realmRepresentation to update the security
        configureRealm(realmRepresentation);
        realmResource.update(realmRepresentation);
        createOpenRemoteClientApplication(realmRepresentation.getRealm());
        publishModification(PersistenceEvent.Cause.CREATE, tenant);
        return convert(Container.JSON, Tenant.class, realmRepresentation);
    }

    @Override
    public void deleteTenant(String realm) {
        Tenant tenant = getTenant(realm);

        if (tenant != null) {
            LOG.fine("Delete tenant: " + realm);
            getRealms().realm(realm).remove();
            publishModification(PersistenceEvent.Cause.DELETE, tenant);
        }
    }

    // TODO: Provide an implementation agnostic client
    public ClientRepresentation getClient(String realm, String clientId) {
        ClientsResource clientsResource = getRealms().realm(realm).clients();
        List<ClientRepresentation> clients = clientsResource.findByClientId(clientId);
        if (clients.isEmpty()) {
            return null;
        }

        // Need to get secret separately for some reason
        ClientRepresentation client = clients.get(0);
        CredentialRepresentation credentialRepresentation = clientsResource.get(client.getId()).getSecret();
        if (credentialRepresentation != null) {
            client.setSecret(credentialRepresentation.getValue());
        }
        return client;
    }

    // TODO: Provide an implementation agnostic client
    public ClientRepresentation[] getClients(String realm) {
        return getRealms().realm(realm).clients().findAll().toArray(new ClientRepresentation[0]);
    }

    // TODO: Provide an implementation agnostic client
    public ClientRepresentation createClient(String realm, ClientRepresentation client) {
        ClientsResource clientsResource = getRealms().realm(realm).clients();
        Response response = clientsResource.create(client);
        response.close();
        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            return null;
        }

        client = clientsResource.findByClientId(client.getClientId()).get(0);
        ClientResource clientResource = clientsResource.get(client.getId());
        addDefaultRoles(clientResource.roles());
        return client;
    }

    // TODO: Provide an implementation agnostic client
    public void updateClient(String realm, ClientRepresentation client) {
        getRealms().realm(realm).clients().get(client.getId()).update(client);
    }

    public void deleteClient(String realm, String clientId) {
        getRealms().realm(realm).clients().findByClientId(clientId).forEach(client ->
            deleteClient(realm, client));
    }

    public void deleteClient(String realm, ClientRepresentation client) {
        getRealms().realm(realm).clients().get(client.getId()).remove();
    }

    public User getClientServiceUser(String realm, String clientId) {
        ClientRepresentation client = getClient(realm, clientId);
        if (client == null) {
            return null;
        }
        UserRepresentation user = getRealms().realm(realm).clients().get(client.getId()).getServiceAccountUser();
        return user != null ? convert(Container.JSON, User.class, user) : null;
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

    public void configureRealm(RealmRepresentation realmRepresentation) {

        realmRepresentation.setAccessTokenLifespan(Constants.ACCESS_TOKEN_LIFESPAN_SECONDS);

        String themeName = getString(container.getConfig(), realmRepresentation.getRealm().toUpperCase(Locale.ROOT) + REALM_KEYCLOAK_THEME_SUFFIX, getString(container.getConfig(), DEFAULT_REALM_KEYCLOAK_THEME, DEFAULT_REALM_KEYCLOAK_THEME_DEFAULT));

        if(TextUtil.isNullOrEmpty(realmRepresentation.getLoginTheme())) {
            realmRepresentation.setLoginTheme(themeName);
        }
        if(TextUtil.isNullOrEmpty(realmRepresentation.getAccountTheme())) {
            realmRepresentation.setAccountTheme(themeName);
        }
        if(TextUtil.isNullOrEmpty(realmRepresentation.getEmailTheme())) {
            realmRepresentation.setEmailTheme(themeName);
        }

        realmRepresentation.setDisplayNameHtml(
            realmRepresentation.getDisplayName().replaceAll("[^A-Za-z0-9]", "")
        );
        realmRepresentation.setSsoSessionIdleTimeout(sessionTimeoutSeconds);
        realmRepresentation.setSsoSessionMaxLifespan(sessionMaxSeconds);
        realmRepresentation.setOfflineSessionIdleTimeout(sessionOfflineTimeoutSeconds);

        // Service-internal network (between manager and keycloak service containers) does not use SSL
        realmRepresentation.setSslRequired(SslRequired.NONE.toString());

        // Configure SMTP
        String host = container.getConfig().getOrDefault(SETUP_EMAIL_HOST, null);
        if (!TextUtil.isNullOrEmpty(host) && (realmRepresentation.getSmtpServer() == null || realmRepresentation.getSmtpServer().isEmpty())) {
            LOG.info("Configuring Keycloak SMTP settings for realm: " + realmRepresentation.getRealm());
            Map<String, String> emailConfig = new HashMap<>();
            emailConfig.put("host", host);
            emailConfig.put("port", container.getConfig().getOrDefault(SETUP_EMAIL_PORT, Integer.toString(SETUP_EMAIL_PORT_DEFAULT)));
            emailConfig.put("user", container.getConfig().getOrDefault(SETUP_EMAIL_USER, null));
            emailConfig.put("password", container.getConfig().getOrDefault(SETUP_EMAIL_PASSWORD, null));
            emailConfig.put("auth", container.getConfig().containsKey(SETUP_EMAIL_USER) ? "true" : "false");
            emailConfig.put("tls", Boolean.toString(getBoolean(container.getConfig(), SETUP_EMAIL_TLS, SETUP_EMAIL_TLS_DEFAULT)));
            emailConfig.put("from", getString(container.getConfig(), SETUP_EMAIL_FROM_KEYCLOAK, SETUP_EMAIL_FROM_KEYCLOAK_DEFAULT + getString(container.getConfig(), IDENTITY_NETWORK_HOST, IDENTITY_NETWORK_HOST_DEFAULT)));
            realmRepresentation.setSmtpServer(emailConfig);
        }

        // Configure CSP header
        Map<String, String> headers = realmRepresentation.getBrowserSecurityHeaders();
        if (headers == null) {
            headers = new HashMap<>();
            realmRepresentation.setBrowserSecurityHeaders(headers);
        }

        if (container.isDevMode()) {
                headers.computeIfPresent("contentSecurityPolicy", (hdrName, hdrValue) -> "frame-src *; frame-ancestors *; object-src 'none'");
        } else {
            String allowedOriginsStr = getString(container.getConfig(), WEBSERVER_ALLOWED_ORIGINS, WEBSERVER_ALLOWED_ORIGINS_DEFAULT);
            if (!TextUtil.isNullOrEmpty(allowedOriginsStr)) {
                headers.compute("contentSecurityPolicy", (hdrName, hdrValue) ->
                        "frame-src 'self' " +
                        allowedOriginsStr.replace(';', ' ') +
                        "; frame-ancestors 'self' " +
                        allowedOriginsStr.replace(';', ' ') +
                        "; object-src 'none'");
            }
        }
    }

    public void createOpenRemoteClientApplication(String realm) {
        createClient(realm, createDefaultClientRepresentation(realm, KEYCLOAK_CLIENT_ID, "OpenRemote", devMode));
    }

    /**
     * Keycloak only allows realm CRUD using the {realm}-realm client or the admin-cli client so we need to ensure we
     * have a token for one of these realms; if we are creating a realm then that means using the admin-cli
     */
    protected String getAdminAccessToken() {
        return getKeycloak().getAccessToken(
            MASTER_REALM, new PasswordAuthForm(ADMIN_CLI_CLIENT_ID, MASTER_REALM_ADMIN_USER, keycloakAdminPassword)
        ).getToken();
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

    public String addLDAPConfiguration(String realm, ComponentRepresentation componentRepresentation) {
        ClientRequestInfo clientRequestInfo = new ClientRequestInfo(null, getAdminAccessToken());

        RealmResource realmResource = getRealms()
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

    public String addLDAPMapper(String realm, ComponentRepresentation componentRepresentation) {
        ClientRequestInfo clientRequestInfo = new ClientRequestInfo(null, getAdminAccessToken());
        RealmResource realmResource = getRealms()
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
