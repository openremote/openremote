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

import io.undertow.util.Headers;
import org.apache.camel.ExchangePattern;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.*;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.representations.idm.*;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.persistence.PersistenceEvent;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.security.AuthContext;
import org.openremote.container.security.keycloak.KeycloakIdentityProvider;
import org.openremote.container.timer.TimerService;
import org.openremote.container.web.WebService;
import org.openremote.manager.apps.ConsoleAppService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.model.Constants;
import org.openremote.model.Container;
import org.openremote.model.auth.OAuthGrant;
import org.openremote.model.event.shared.TenantFilter;
import org.openremote.model.query.UserQuery;
import org.openremote.model.query.filter.TenantPredicate;
import org.openremote.model.security.*;
import org.openremote.model.util.TextUtil;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.openremote.container.util.MapAccess.getBoolean;
import static org.openremote.container.util.MapAccess.getString;
import static org.openremote.container.web.WebService.WEBSERVER_ALLOWED_ORIGINS;
import static org.openremote.container.web.WebService.WEBSERVER_ALLOWED_ORIGINS_DEFAULT;
import static org.openremote.manager.setup.AbstractKeycloakSetup.SETUP_EMAIL_FROM_KEYCLOAK;
import static org.openremote.manager.setup.AbstractKeycloakSetup.SETUP_EMAIL_FROM_KEYCLOAK_DEFAULT;
import static org.openremote.model.Constants.*;
import static org.openremote.model.value.Values.convert;

/**
 * All keycloak interaction is done through the admin-cli client; security is implemented downstream of here; anything
 * in the same process as this service has privileged access to keycloak.
 */
public class ManagerKeycloakIdentityProvider extends KeycloakIdentityProvider implements ManagerIdentityProvider {

    private static final Logger LOG = Logger.getLogger(ManagerKeycloakIdentityProvider.class.getName());
    public static final String REALM_KEYCLOAK_THEME_SUFFIX = "_REALM_KEYCLOAK_THEME";
    public static final String DEFAULT_REALM_KEYCLOAK_THEME = "DEFAULT_REALM_KEYCLOAK_THEME";
    public static final String DEFAULT_REALM_KEYCLOAK_THEME_DEFAULT = "openremote";

    protected PersistenceService persistenceService;
    protected TimerService timerService;
    protected MessageBrokerService messageBrokerService;
    protected ClientEventService clientEventService;
    protected ConsoleAppService consoleAppService;
    protected String keycloakAdminPassword;
    protected Container container;

    public ManagerKeycloakIdentityProvider(OAuthGrant grant) {
        super(grant);
    }

    @Override
    public void init(Container container) {
        super.init(container);
        this.container = container;
        this.keycloakAdminPassword = container.getConfig().getOrDefault(SETUP_ADMIN_PASSWORD, SETUP_ADMIN_PASSWORD_DEFAULT);
        this.timerService = container.getService(TimerService.class);
        this.persistenceService = container.getService(PersistenceService.class);
        this.messageBrokerService = container.getService(MessageBrokerService.class);
        this.clientEventService = container.getService(ClientEventService.class);
        this.consoleAppService = container.getService(ConsoleAppService.class);
    }

    @Override
    public void start(Container container) {
        super.start(container);
        if (container.isDevMode()) {
            enableAuthProxy(container.getService(WebService.class));
        }
    }

    @Override
    protected void addClientRedirectUris(String client, List<String> redirectUrls, boolean devMode) {
        // Callback URL used by Manager web client authentication, any relative path to "ourselves" is fine
        String realmManagerCallbackUrl = UriBuilder.fromUri("/").path(client).path("*").build().toString();
        redirectUrls.add(realmManagerCallbackUrl);
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
    public User createUpdateUser(String realm, final User user, String passwordSecret) throws WebApplicationException {
        return getRealms(realmsResource -> {

            if (user.getUsername() == null) {
                throw new BadRequestException("Attempt to create/update user but no username provided: User=" + user);
            }

            boolean isUpdate = false;
            User existingUser = user.getId() != null ? getUser(realm, user.getId()) : getUserByUsername(realm, user.getUsername());
            ClientRepresentation clientRepresentation = null;
            UserRepresentation userRepresentation = null;

            if (existingUser == null && user.isServiceAccount()) {
                // Could be a service user
                clientRepresentation = realmsResource.realm(realm)
                    .clients()
                    .findByClientId(user.getUsername()).stream().findFirst().orElse(null);

                userRepresentation = clientRepresentation != null ? realmsResource.realm(realm)
                    .clients()
                    .get(user.getUsername()).getServiceAccountUser() : null;

                if (userRepresentation != null) {
                    existingUser = convert(userRepresentation, User.class);
                    existingUser.setServiceAccount(true);
                } else if (clientRepresentation != null) {
                    String msg = "Attempt to update/creat service user but a regular client with same client ID as this username already exists: User=" + user;
                    LOG.info(msg);
                    throw new NotAllowedException(msg);
                }
            }

            if (existingUser != null && user.getId() != null && !existingUser.getId().equals(user.getId())) {
                String msg = "Attempt to update user but retrieved user ID doesn't match supplied so ignoring: User=" + user;
                LOG.info(msg);
                throw new BadRequestException(msg);
            }

            if (existingUser != null) {
                isUpdate = true;

                if (existingUser.isServiceAccount() != user.isServiceAccount()) {
                    String msg = "Attempt to update user service account flag not allowed: User=" + user;
                    LOG.info(msg);
                    throw new NotAllowedException(msg);
                }

                if (existingUser.isServiceAccount() && !existingUser.getUsername().equals(user.getUsername())) {
                    String msg = "Attempt to update username of service user not allowed: User=" + user;
                    LOG.info(msg);
                    throw new NotAllowedException(msg);
                }
            }

            // For service users we don't actually create the user - keycloak does that when the client is created
            if (isUpdate) {

                if (user.isServiceAccount()) {

                    if (passwordSecret != null) {
                        if (clientRepresentation == null) {
                            clientRepresentation = realmsResource.realm(realm)
                                .clients()
                                .get(user.getUsername()).toRepresentation();
                        }

                        if (clientRepresentation == null) {
                            String msg = "Attempt to update service user secret failed to find client: User=" + user;
                            LOG.info(msg);
                            throw new BadRequestException(msg);
                        }
                        clientRepresentation.setSecret(passwordSecret);
                        createUpdateClient(realm, clientRepresentation);
                    } else {
                        String msg = "Attempt to update service user but only secret can be udpated and no new value provided: User=" + user;
                        LOG.info(msg);
                        throw new BadRequestException(msg);
                    }

                } else {

                    // User only has a subset of user representation so overlay on actual user representation
                    UserResource userResource = realmsResource.realm(realm).users().get(user.getId());
                    userRepresentation = userResource.toRepresentation();
                    userRepresentation.setUsername(user.getUsername());
                    userRepresentation.setFirstName(user.getFirstName());
                    userRepresentation.setLastName(user.getLastName());
                    userRepresentation.setEmail(user.getEmail());
                    userRepresentation.setEnabled(user.getEnabled());
                    userResource.update(userRepresentation);

                }
            } else {

                if (user.isServiceAccount()) {

                    // Just create client with service account and user will be generated
                    clientRepresentation = new ClientRepresentation();
                    clientRepresentation.setStandardFlowEnabled(false);
                    clientRepresentation.setImplicitFlowEnabled(false);
                    clientRepresentation.setDirectAccessGrantsEnabled(false);
                    clientRepresentation.setServiceAccountsEnabled(true);
                    clientRepresentation.setClientAuthenticatorType("client-secret");
                    clientRepresentation.setClientId(user.getUsername());
                    clientRepresentation.setSecret(passwordSecret);
                    clientRepresentation = createUpdateClient(realm, clientRepresentation);
                    userRepresentation = realmsResource.realm(realm)
                        .clients()
                        .get(clientRepresentation.getId()).getServiceAccountUser();
                } else {

                    userRepresentation = convert(user, UserRepresentation.class);
                    RealmResource realmResource = realmsResource.realm(realm);
                    Response response = realmResource.users().create(userRepresentation);
                    String location = response.getHeaderString(Headers.LOCATION_STRING);
                    response.close();
                    if (!response.getStatusInfo().equals(Response.Status.CREATED) || TextUtil.isNullOrEmpty(location)) {
                        throw new BadRequestException("Failed to create user: User=" + user);
                    }
                    String[] locationArr = location.split("/");
                    String userId = locationArr.length > 0 ? locationArr[locationArr.length-1] : null;
                    userRepresentation = realmResource.users().get(userId).toRepresentation();

                    if (passwordSecret != null) {
                        CredentialRepresentation credentials = new CredentialRepresentation();
                        credentials.setType("password");
                        credentials.setValue(passwordSecret);
                        credentials.setTemporary(false);
                        realmResource.users().get(userRepresentation.getId()).resetPassword(credentials);
                    }

                }
            }

            User updatedUser = convert(userRepresentation, User.class);
            if (updatedUser != null) {
                user.setServiceAccount(userRepresentation.getUsername().startsWith(User.SERVICE_ACCOUNT_PREFIX));
                updatedUser.setRealm(realm);
            }
            return updatedUser;
        });
    }

    @Override
    public void deleteUser(String realm, String userId) {
        getRealms(realmsResource -> {
            Response response = realmsResource.realm(realm).users().delete(userId);
            response.close();
            if (!response.getStatusInfo().equals(Response.Status.NO_CONTENT)) {
                throw new IllegalStateException("Failed to delete user: " + userId);
            }
            return null;
        });
    }

    @Override
    public void resetPassword(String realm, String userId, Credential credential) {
        getRealms(realmsResource -> {
            realmsResource.realm(realm).users().get(userId).resetPassword(
                convert(credential, CredentialRepresentation.class)
            );
            return null;
        });
    }

    @Override
    public Role[] getRoles(String realm, String client) {
        return getRealms(realmsResource -> {
            RealmResource realmResource = realmsResource.realm(realm);
            ClientsResource clientsResource = realmResource.clients();
            ClientRepresentation clientRepresentation = getClient(realm, client);
            if (clientRepresentation == null) {
                throw new IllegalStateException("Cannot find specified client: " + client);
            }
            ClientResource clientResource = clientsResource.get(clientRepresentation.getId());
            List<RoleRepresentation> clientRoles = clientResource.roles().list();
            List<Role> roles = new ArrayList<>();

            for (RoleRepresentation clientRole : clientRoles) {
                String[] composites = clientRole.isComposite() ? realmResource.rolesById().getClientRoleComposites(clientRole.getId(), clientRepresentation.getId()).stream().map(RoleRepresentation::getId).toArray(String[]::new) : null;
                roles.add(new Role(clientRole.getId(), clientRole.getName(), clientRole.isComposite(), null, composites).setDescription(clientRole.getDescription()));
            }

            return roles.toArray(new Role[0]);
        });
    }

    @Override
    public void updateClientRoles(String realm, String clientId, Role[] roles) {

        getRealms(realmsResource -> {
            RealmResource realmResource = realmsResource.realm(realm);
            ClientsResource clientsResource = realmResource.clients();
            ClientRepresentation clientRepresentation = getClient(realm, clientId);
            if (clientRepresentation == null) {
                throw new IllegalStateException("Cannot find specified client: " + clientId);
            }
            ClientResource clientResource = clientsResource.get(clientRepresentation.getId());
            List<RoleRepresentation> existingRoles = new ArrayList<>(clientResource.roles().list());

            List<RoleRepresentation> removedRoles = existingRoles.stream()
                .filter(existingRole -> Arrays.stream(roles).noneMatch(r -> existingRole.getId().equals(r.getId())))
                .collect(Collectors.toList());

            removedRoles.forEach(removedRole -> {
                realmResource.rolesById().deleteRole(removedRole.getId());
                existingRoles.remove(removedRole);
            });

            Arrays.stream(roles).forEach(role -> {

                RoleRepresentation existingRole;
                boolean compositesModified = false;
                Set<RoleRepresentation> existingComposites = new HashSet<>();
                Set<RoleRepresentation> requestedComposites = new HashSet<>();

                if (role.getId() == null) {
                    existingRole = saveClientRole(realmResource, clientResource, role, null);
                    existingRoles.add(existingRole);
                    compositesModified = role.getCompositeRoleIds() != null && role.getCompositeRoleIds().length > 0;
                    if (compositesModified) {
                        requestedComposites.addAll(Arrays.stream(role.getCompositeRoleIds())
                            .map(id -> existingRoles.stream().filter(er -> er.getId().equals(id)).findFirst().orElse(null))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet()));
                    }
                } else {
                    existingRole = existingRoles.stream().filter(r -> r.getId().equals(role.getId())).findFirst().orElseThrow(() -> new IllegalStateException("One or more supplied roles have an ID that doesn't exist"));

                    boolean isComposite = role.isComposite() && role.getCompositeRoleIds() != null && role.getCompositeRoleIds().length > 0;

                    boolean rolePropertiesModified = !Objects.equals(existingRole.getName(), role.getName())
                        || !Objects.equals(existingRole.getDescription(), role.getDescription());

                    if (isComposite || existingRole.isComposite()) {
                        existingComposites.addAll(Optional.ofNullable(realmResource.rolesById().getClientRoleComposites(existingRole.getId(), clientRepresentation.getId())).orElse(new HashSet<>()));
                        requestedComposites.addAll(Arrays.stream(role.getCompositeRoleIds())
                            .map(id -> existingRoles.stream().filter(er -> er.getId().equals(id)).findFirst().orElse(null))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet()));

                        if (requestedComposites.size() != role.getCompositeRoleIds().length) {
                            throw new IllegalStateException("One or more composite roles contain an invalid role ID");
                        }

                        compositesModified = !Objects.equals(existingComposites, requestedComposites);
                    }

                    if (rolePropertiesModified) {
                        // Merge the role property changes
                        saveClientRole(realmResource, clientResource, role, existingRole);
                    }
                }

                if (compositesModified) {
                    List<RoleRepresentation> removed = existingComposites.stream().filter(existing -> !requestedComposites.contains(existing)).collect(Collectors.toList());
                    List<RoleRepresentation> added = requestedComposites.stream().filter(existing -> !existingComposites.contains(existing)).collect(Collectors.toList());
                    if (!removed.isEmpty()) {
                        realmResource.rolesById().deleteComposites(existingRole.getId(), removed);
                    }
                    if (!added.isEmpty()) {
                        realmResource.rolesById().addComposites(existingRole.getId(), added);
                    }
                }
            });

            return null;
        });
    }

    protected RoleRepresentation saveClientRole(RealmResource realmResource, ClientResource clientResource, Role role, RoleRepresentation representation) {
        if (representation == null) {
            representation = new RoleRepresentation();
        }
        representation.setName(role.getName());
        representation.setDescription(role.getDescription());
        representation.setClientRole(true);
        if (representation.getId() == null) {
            clientResource.roles().create(representation);
        } else {
            realmResource.rolesById().updateRole(representation.getId(), representation);
        }

        return clientResource.roles().get(representation.getName()).toRepresentation();
    }

    @Override
    public Role[] getUserRoles(String realm, String userId, String client) {
        return getRealms(realmsResource -> {
            RealmResource realmResource = realmsResource.realm(realm);
            RoleMappingResource roleMappingResource = realmResource.users().get(userId).roles();
            ClientsResource clientsResource = realmResource.clients();
            String clientId = clientsResource.findByClientId(client).get(0).getId();
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
                    isAssigned,
                    null)
                    .setDescription(roleRepresentation.getDescription()));
            }

            return roles.toArray(new Role[0]);
        });
    }

    @Override
    public void updateUserRoles(String realm, String userId, String client, String...roles) {
        getRealms(realmsResource -> {
            RealmResource realmResource = realmsResource.realm(realm);
            UserRepresentation user = realmResource.users().get(userId).toRepresentation();

            if (user == null) {
                throw new IllegalStateException("Multiple users with the same username found");
            }

            RoleMappingResource roleMappingResource = realmResource.users().get(user.getId()).roles();
            ClientRepresentation clientRepresentation = getClient(realm, client);

            // Get all role mappings for user on this client and remove any no longer in the roles
            List<RoleRepresentation> clientMappedRoles = roleMappingResource.clientLevel(clientRepresentation.getId()).listAll();
            List<RoleRepresentation> availableRoles = roleMappingResource.clientLevel(clientRepresentation.getId()).listAvailable();

            // Get newly defined roles
            List<RoleRepresentation> addRoles = roles == null ? Collections.emptyList() : Arrays.stream(roles)
                .filter(cr -> clientMappedRoles.stream().noneMatch(r -> r.getName().equals(cr)))
                .map(cr -> availableRoles.stream().filter(r -> r.getName().equals(cr)).findFirst().orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            // Remove obsolete roles
            List<RoleRepresentation> removeRoles = roles == null ? clientMappedRoles : clientMappedRoles.stream()
                .filter(r -> Arrays.stream(roles).noneMatch(cr -> cr.equals(r.getName())))
                .collect(Collectors.toList());

            if (!removeRoles.isEmpty()) {
                roleMappingResource.clientLevel(clientRepresentation.getId()).remove(removeRoles);
            }
            if (!addRoles.isEmpty()) {
                roleMappingResource.clientLevel(clientRepresentation.getId()).add(addRoles);
            }

            return null;
        });
    }

    @Override
    public boolean isMasterRealmAdmin(String userId) {
        List<UserRepresentation> adminUsers = getRealms(realmsResource ->
            realmsResource.realm(MASTER_REALM)
                .users()
                .search(MASTER_REALM_ADMIN_USER, null, null));


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
        try {
            RealmRepresentation realmRepresentation = getRealms(realmsResource ->
                realmsResource.realm(realm).toRepresentation());
            return convert(realmRepresentation, Tenant.class);
        } catch (Exception ex) {
            LOG.log(Level.INFO, "Failed to get tenant for realm: " + realm, ex);
        }
        return null;
    }

    @Override
    public void updateTenant(Tenant tenant) {
        LOG.fine("Update tenant: " + tenant);
        getRealms(realmsResource -> {
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
            return null;
        });
    }

    @Override
    public Tenant createTenant(Tenant tenant) {
        LOG.fine("Create tenant: " + tenant);
        return getRealms(realmsResource -> {
            RealmRepresentation realmRepresentation = convert(tenant, RealmRepresentation.class);
            realmsResource.create(realmRepresentation);
            RealmResource realmResource = realmsResource.realm(tenant.getRealm());

            realmRepresentation = realmResource.toRepresentation();
            // Need a committed realmRepresentation to update the security
            configureRealm(realmRepresentation);
            realmResource.update(realmRepresentation);

            // Auto create the standard openremote client
            ClientRepresentation clientRepresentation = generateOpenRemoteClientRepresentation();
            createUpdateClient(tenant.getRealm(), clientRepresentation);
            publishModification(PersistenceEvent.Cause.CREATE, tenant);
            return convert(realmRepresentation, Tenant.class);
        });
    }

    @Override
    public void deleteTenant(String realm) {
        Tenant tenant = getTenant(realm);

        if (tenant != null) {
            LOG.fine("Delete tenant: " + realm);
            getRealms(realmsResource -> {
                realmsResource.realm(realm).remove();
                return null;
            });
            publishModification(PersistenceEvent.Cause.DELETE, tenant);
        }
    }

    public ClientRepresentation generateOpenRemoteClientRepresentation() {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(KEYCLOAK_CLIENT_ID);
        client.setName("OpenRemote");
        client.setPublicClient(true);

        if (container.isDevMode()) {
            // We need direct access for integration tests
            LOG.info("### Allowing direct access grants for client id '" + client.getClientId() + "', this must NOT be used in production! ###");
            client.setDirectAccessGrantsEnabled(true);

            // Allow any web origin (this will add CORS headers to token requests etc.)
            client.setWebOrigins(Collections.singletonList("*"));
            client.setRedirectUris(Collections.singletonList("*"));
        } else {
            // TODO: Decide how clients should be handled
            client.setWebOrigins(Collections.singletonList("+"));
            client.setRedirectUris(Collections.singletonList("/*"));
//            List<String> redirectUris = new ArrayList<>();
//            try {
//                for (String consoleName : consoleAppService.getInstalled()) {
//                    addClientRedirectUris(consoleName, redirectUris, devMode);
//                }
//            } catch (Exception exception) {
//                LOG.log(Level.WARNING, exception.getMessage(), exception);
//                addClientRedirectUris(realm, redirectUris, devMode);
//            }
//
//            client.setRedirectUris(redirectUris);
        }

        return client;
    }

    // TODO: Provide an implementation agnostic client
    public ClientRepresentation getClient(String realm, String clientId) {
        return getRealms(realmsResource -> {
            ClientsResource clientsResource = realmsResource.realm(realm).clients();
            List<ClientRepresentation> clients = clientsResource.findByClientId(clientId);

            if (clients.isEmpty()) {
                return null;
            }

            ClientRepresentation client = clients.get(0);
            if ("client-secret".equals(client.getClientAuthenticatorType())) {
                // Need to get secret separately for some reason
                CredentialRepresentation credentialRepresentation = clientsResource.get(client.getId()).getSecret();
                if (credentialRepresentation != null) {
                    client.setSecret(credentialRepresentation.getValue());
                }
            }
            return client;
        });
    }

    // TODO: Provide an implementation agnostic client
    public ClientRepresentation[] getClients(String realm) {
        return getRealms(realmsResource -> realmsResource.realm(realm).clients().findAll().toArray(new ClientRepresentation[0]));
    }

    // TODO: Provide an implementation agnostic client
    public ClientRepresentation createUpdateClient(String realm, ClientRepresentation client) {

        if (client == null || client.getClientId() == null) {
            throw new IllegalArgumentException("Client is null or clientId is missing");
        }

        boolean isUpdate = !TextUtil.isNullOrEmpty(client.getId());

        if (!isUpdate) {
            // Check if client exists
            ClientRepresentation clientRepresentation = getClient(realm, client.getClientId());
            if (clientRepresentation != null) {
                client.setId(clientRepresentation.getId());
                isUpdate = true;
            }
        }

        boolean finalIsUpdate = isUpdate;

        return getRealms(realmsResource -> {
            ClientsResource clientsResource = realmsResource.realm(realm).clients();
            if (finalIsUpdate) {
                clientsResource.get(client.getId()).update(client);
            } else {
                Response response = clientsResource.create(client);
                response.close();
                if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                    return null;
                }

                ClientRepresentation newClient = clientsResource.findByClientId(client.getClientId()).get(0);
                ClientResource clientResource = clientsResource.get(newClient.getId());
                addDefaultRoles(clientResource.roles());
                return newClient;
            }
            return null;
        });
    }

    public void deleteClient(String realm, String clientId) {

        if (TextUtil.isNullOrEmpty(realm)
            || TextUtil.isNullOrEmpty(clientId)) {
            throw new IllegalArgumentException("Invalid client credentials realm and client ID must be specified");
        }

        getRealms(realmsResource -> {
            RealmResource realmResource = realmsResource.realm(realm);

            if (realmResource == null) {
                LOG.info("Invalid realm provided for deleteClient call: " + realm);
                return null;
            }

            LOG.info("Deleting client: realm=" + realm + ", client ID=" + clientId);
            ClientsResource clients = realmResource.clients();
            clients.findByClientId(clientId).forEach(client -> clients.get(client.getId()).remove());
            return null;
        });
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
    public boolean canSubscribeWith(AuthContext auth, TenantFilter<?> filter, ClientRole... requiredRoles) {
        // Superuser can always subscribe
        if (auth.isSuperUser())
            return true;

        // Restricted users get nothing
        if (isRestrictedUser(auth.getUserId()))
            return false;

        // User must have role
        if (requiredRoles != null) {
            for (ClientRole requiredRole : requiredRoles) {
                if (!auth.hasResourceRole(requiredRole.getValue(), auth.getClientId())) {
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
            emailConfig.put("from", getString(container.getConfig(), SETUP_EMAIL_FROM_KEYCLOAK, SETUP_EMAIL_FROM_KEYCLOAK_DEFAULT));
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

    protected void publishModification(PersistenceEvent.Cause cause, Tenant tenant) {
        // Fire persistence event although we don't use database for Tenant CUD but call Keycloak API
        PersistenceEvent<?> persistenceEvent = new PersistenceEvent<>(cause, tenant, new String[0], null);

        if (messageBrokerService.getProducerTemplate() != null) {
            messageBrokerService.getProducerTemplate().sendBodyAndHeader(
                PersistenceEvent.PERSISTENCE_TOPIC,
                ExchangePattern.InOnly,
                persistenceEvent,
                PersistenceEvent.HEADER_ENTITY_TYPE,
                persistenceEvent.getEntity().getClass()
            );
        }
    }

    public String addLDAPConfiguration(String realm, ComponentRepresentation componentRepresentation) {

        return getRealms(realmsResource -> {
            RealmResource realmResource = realmsResource.realm(realm);
            Response response = realmResource.components().add(componentRepresentation);
            response.close();

            if (!response.getStatusInfo().equals(Response.Status.CREATED)) {
                throw new IllegalStateException("Failed to add LDAP configuration");
            } else {
                ComponentRepresentation newComponentRepresentation = realmResource.components()
                    .query(componentRepresentation.getParentId(),
                        componentRepresentation.getProviderType(),
                        componentRepresentation.getName()).get(0);
                syncUsers(newComponentRepresentation.getId(), realm, "triggerFullSync");
                return newComponentRepresentation.getId();
            }
        });
    }

    public String addLDAPMapper(String realm, ComponentRepresentation componentRepresentation) {
        return getRealms(realmsResource -> {
            RealmResource realmResource = realmsResource.realm(realm);
            Response response = realmResource.components().add(componentRepresentation);
            response.close();

            if (!response.getStatusInfo().equals(Response.Status.CREATED)) {
                throw new IllegalStateException("Failed to add LDAP mapper");
            } else {
                ComponentRepresentation newComponentRepresentation = realmResource.components()
                    .query(componentRepresentation.getParentId(),
                        componentRepresentation.getProviderType(),
                        componentRepresentation.getName()).get(0);
                realmResource.userStorage().syncMapperData(newComponentRepresentation.getParentId(), newComponentRepresentation.getId(), "fedToKeycloak");
                return newComponentRepresentation.getId();
            }
        });
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{}";
    }
}
