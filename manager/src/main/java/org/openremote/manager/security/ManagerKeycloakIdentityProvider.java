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
import org.apache.commons.io.IOUtils;
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
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.UserQuery;
import org.openremote.model.query.filter.StringPredicate;
import org.openremote.model.query.filter.TenantPredicate;
import org.openremote.model.security.*;
import org.openremote.model.util.TextUtil;
import org.openremote.model.util.ValueUtil;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;
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
import static org.openremote.model.util.ValueUtil.convert;

/**
 * All keycloak interaction is done through the admin-cli client; security is implemented downstream of here; anything
 * in the same process as this service has privileged access to keycloak.
 */
public class ManagerKeycloakIdentityProvider extends KeycloakIdentityProvider implements ManagerIdentityProvider {

    private static final Logger LOG = Logger.getLogger(ManagerKeycloakIdentityProvider.class.getName());
    public static final String REALM_KEYCLOAK_THEME_SUFFIX = "_REALM_KEYCLOAK_THEME";
    public static final String DEFAULT_REALM_KEYCLOAK_THEME = "DEFAULT_REALM_KEYCLOAK_THEME";
    public static final String DEFAULT_REALM_KEYCLOAK_THEME_DEFAULT = "openremote";
    public static final String KEYCLOAK_GRANT_FILE = "KEYCLOAK_GRANT_FILE";
    public static final String KEYCLOAK_GRANT_FILE_DEFAULT = "manager/build/keycloak.json";
    public static final String KEYCLOAK_DEFAULT_ROLES_PREFIX = "default-roles-";

    protected PersistenceService persistenceService;
    protected TimerService timerService;
    protected MessageBrokerService messageBrokerService;
    protected ClientEventService clientEventService;
    protected ConsoleAppService consoleAppService;
    protected String keycloakAdminPassword;
    protected Container container;

    @Override
    public void init(Container container) {
        super.init(container);
        this.container = container;
        OAuthGrant grant = loadCredentials();

        // Update the keycloak proxy credentials to use stored credentials
        if (grant != null) {
            setActiveCredentials(grant);
        }

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
        User[] users = getUsers(new UserQuery().tenant(new TenantPredicate(realm)));

        return getRealms(realmsResource -> {
            // Need to load secrets from client resource
            return Arrays.stream(users).filter(user -> {
                RealmResource realmResource = realmsResource.realm(realm);

                // Filter out users with service account attribute
                UserRepresentation userRepresentation = realmResource.users().get(user.getId()).toRepresentation();
                if (userRepresentation.getAttributes() != null && userRepresentation.getAttributes().containsKey(User.SYSTEM_ACCOUNT_ATTRIBUTE)) {
                    return false;
                }

                return true;
            }).toArray(User[]::new);
        });
    }

    @Override
    public User[] getServiceUsers(String realm) {
        User[] users = ManagerIdentityProvider.getUsersFromDb(persistenceService, new UserQuery().usernames(new StringPredicate(AssetQuery.Match.BEGIN, User.SERVICE_ACCOUNT_PREFIX)).tenant(new TenantPredicate(realm)));

        return getRealms(realmsResource -> Arrays.stream(users).filter(user -> {
            RealmResource realmResource = realmsResource.realm(realm);

            // Filter out users with system account attribute
            UserRepresentation userRepresentation = realmResource.users().get(user.getId()).toRepresentation();
            if (userRepresentation.getAttributes() != null && userRepresentation.getAttributes().containsKey(User.SYSTEM_ACCOUNT_ATTRIBUTE)) {
                return false;
            }

            withClientResource(realm, user.getUsername(), realmsResource, (clientRep, clientResource) ->
                    user.setSecret(getClientSecret(clientResource)),
            null);

            return true;
        }).toArray(User[]::new));
    }

    protected String getClientSecret(ClientResource clientResource) {
        CredentialRepresentation credentialRepresentation = clientResource.getSecret();
        return credentialRepresentation != null ? credentialRepresentation.getValue() : null;
    }

    protected <T> T withClientResource(String realm, String client, RealmsResource realmsResource, BiFunction<ClientRepresentation, ClientResource, T> clientResourceConsumer, Supplier<T> notFoundProvider) {
        ClientRepresentation clientRepresentation = null;
        ClientResource clientResource = null;

        try {
            ClientsResource clientsResource = realmsResource.realm(realm).clients();
            List<ClientRepresentation> clientRepresentations = clientsResource.findByClientId(client);
            if (clientRepresentations != null && !clientRepresentations.isEmpty()) {
                if (clientRepresentations.size() > 1) {
                    throw new IllegalStateException("More than one matching client found realm=" + realm + ", client=" + client);
                }
                clientRepresentation = clientRepresentations.get(0);
                clientResource = clientsResource.get(clientRepresentation.getId());
                if (clientRepresentation.isServiceAccountsEnabled()) {
                    clientRepresentation.setSecret(getClientSecret(clientResource));
                }
            }
        } catch (Exception e) {
            LOG.log(Level.INFO, "withClientResource failed", e);
        }
        if (clientResource != null) {
            return clientResourceConsumer.apply(clientRepresentation, clientResource);
        } else if (notFoundProvider != null) {
            return notFoundProvider.get();
        }
        return null;
    }

    @Override
    public User[] getUsers(List<String> userIds) {
        return getUsers(new UserQuery().ids(userIds.toArray(new String[0])));
    }

    @Override
    public User[] getUsers(UserQuery userQuery) {
        if (userQuery.usernames == null) {
            userQuery.usernames = new StringPredicate[1];
            userQuery.usernames(new StringPredicate(AssetQuery.Match.BEGIN, User.SERVICE_ACCOUNT_PREFIX).negate(true));
        } else {
            userQuery.usernames = Arrays.copyOf(userQuery.usernames, userQuery.usernames.length+1);
            userQuery.usernames[userQuery.usernames.length-1] = new StringPredicate(AssetQuery.Match.BEGIN, User.SERVICE_ACCOUNT_PREFIX).negate(true);
        }

        return ManagerIdentityProvider.getUsersFromDb(persistenceService, userQuery);
    }

    @Override
    public User getUser(String realm, String userId) {
        User user = ManagerIdentityProvider.getUserByIdFromDb(persistenceService, realm, userId);
        if (user != null && user.isServiceAccount()) {
            getRealms(realmsResource -> {
                withClientResource(realm, user.getUsername(), realmsResource, (clientRep, clientResource) ->
                    user.setSecret(getClientSecret(clientResource)), null);
                return null;
            });
        }

        return user;
    }

    @Override
    public User getUserByUsername(String realm, String username) {
        User user = ManagerIdentityProvider.getUserByUsernameFromDb(persistenceService, realm, username);
        if (user != null && user.isServiceAccount()) {
            getRealms(realmsResource -> {
                withClientResource(realm, user.getUsername(), realmsResource, (clientRep, clientResource) ->
                    user.setSecret(getClientSecret(clientResource)), null);
                return null;
            });
        }

        return user;
    }

    @Override
    public User createUpdateUser(String realm, final User user, String passwordSecret) throws WebApplicationException {
        return getRealms(realmsResource -> {

            if (user.getUsername() == null) {
                throw new BadRequestException("Attempt to create/update user but no username provided: User=" + user);
            }

            boolean isUpdate = false;
            User existingUser = user.getId() != null ? getUser(realm, user.getId()) : getUserByUsername(realm, user.getUsername());
            ClientRepresentation clientRepresentation;
            UserRepresentation userRepresentation;

            if (existingUser == null && user.isServiceAccount()) {
                // Could be a service user
                userRepresentation = withClientResource(realm, user.getUsername(), realmsResource, (clientRep, clientResource) -> {
                    UserRepresentation userRep = clientResource.getServiceAccountUser();
                    if (userRep == null) {
                        String msg = "Attempt to update/creat service user but a regular client with same client ID as this username already exists: User=" + user;
                        LOG.info(msg);
                        throw new NotAllowedException(msg);
                    }
                    return userRep;
                }, null);

                if (userRepresentation != null) {
                    existingUser = convert(userRepresentation, User.class);
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

                // User only has a subset of user representation so overlay on actual user representation
                UserResource userResource = realmsResource.realm(realm).users().get(existingUser.getId());
                userRepresentation = userResource.toRepresentation();
                userRepresentation.setFirstName(user.getFirstName());
                userRepresentation.setLastName(user.getLastName());
                userRepresentation.setEmail(user.getEmail());
                userRepresentation.setEnabled(user.getEnabled());
                userResource.update(userRepresentation);

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
                    userRepresentation.setEnabled(user.getEnabled());
                    realmsResource.realm(realm).users().get(userRepresentation.getId()).update(userRepresentation);

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
                }
            }

            if (passwordSecret != null || (!isUpdate && user.isServiceAccount())) {
                if (user.isServiceAccount()) {
                    resetSecret(realm, userRepresentation.getId(), passwordSecret);
                } else {
                    Credential credential = new Credential(passwordSecret, false);
                    resetPassword(realm, userRepresentation.getId(), credential);
                }
            }

            if (user.getAttributes() != null) {
                updateUserAttributes(realm, userRepresentation.getId(), user.getAttributes());
            }

            User updatedUser = convert(userRepresentation, User.class);
            if (updatedUser != null) {
                updatedUser.setRealm(realm);
                if (updatedUser.isServiceAccount()) {
                    updatedUser.setSecret(passwordSecret);
                }
            }
            return updatedUser;
        });
    }

    @Override
    public void deleteUser(String realm, String userId) {

        User user = getUser(realm, userId);

        if (user == null) {
            return;
        }

        getRealms(realmsResource -> {

            if (user.isServiceAccount()) {
                // Delete the client
                deleteClient(realm, user.getUsername());
            } else {
                Response response = realmsResource.realm(realm).users().delete(userId);
                response.close();
                if (!response.getStatusInfo().equals(Response.Status.NO_CONTENT)) {
                    throw new IllegalStateException("Failed to delete user: " + userId);
                }
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
    public String resetSecret(String realm, String userId, String secret) {
        return getRealms(realmsResource -> {
            UserRepresentation userRepresentation = null;
            try {
                userRepresentation = realmsResource.realm(realm).users().get(userId).toRepresentation();
            } catch (Exception ignored) {
            }
            if (userRepresentation == null) {
                return null;
            }

            return withClientResource(
                realm,
                userRepresentation.getUsername().substring(User.SERVICE_ACCOUNT_PREFIX.length()),
                realmsResource,
                (clientRep, clientResource) -> {
                    if (TextUtil.isNullOrEmpty(secret)) {
                        CredentialRepresentation credentialRepresentation = clientResource.generateNewSecret();
                        return credentialRepresentation.getValue();
                    } else {
                        clientRep.setSecret(secret);
                        clientResource.update(clientRep);
                        return secret;
                    }
                },
                null
            );
        });
    }

    @Override
    public void updateUserAttributes(String realm, String userId, Map<String, List<String>> attributes) {
        getRealms(realmsResource -> {
            UserResource userResource = realmsResource.realm(realm).users().get(userId);
            UserRepresentation userRepresentation = realmsResource.realm(realm).users().get(userId).toRepresentation();
            userRepresentation.setAttributes(attributes);
            userResource.update(userRepresentation);
            return null;
        });
    }

    @Override
    public Role[] getRoles(String realm, String client) {
        return getRealms(realmsResource -> {
            RealmResource realmResource = realmsResource.realm(realm);
            ClientsResource clientsResource = realmResource.clients();
            ClientResource clientResource = null;

            if (client != null) {
                ClientRepresentation clientRepresentation = getClient(realm, client);

                if (clientRepresentation == null) {
                    throw new IllegalStateException("Cannot find specified client: " + client);
                }
                clientResource = clientsResource.get(clientRepresentation.getId());
            }

            List<RoleRepresentation> roleRepresentations = clientResource != null ? clientResource.roles().list() : realmResource.roles().list();
            List<Role> roles = new ArrayList<>();

            for (RoleRepresentation clientRole : roleRepresentations) {
                String[] composites = clientRole.isComposite() ? realmResource.rolesById().getRoleComposites(clientRole.getId()).stream().map(RoleRepresentation::getId).toArray(String[]::new) : null;
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

            return withClientResource(realm, client, realmsResource, (clientRepresentation, clientResource) -> {
                RolesResource rolesResource = clientResource.roles();
                List<RoleRepresentation> allRoles = rolesResource.list();
                List<RoleRepresentation> effectiveRoles = roleMappingResource.clientLevel(clientRepresentation.getId()).listEffective();

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
            }, null);
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
            ClientRepresentation clientRepresentation = null;
            ClientResource clientResource = null;

            if (client != null) {
                clientRepresentation = getClient(realm, client);

                if (clientRepresentation == null) {
                    throw new IllegalStateException("Invalid client: " + client);
                }
                clientResource = realmResource.clients().get(clientRepresentation.getId());
            }

            // Get all roles
            List<RoleRepresentation> existingRoles = clientRepresentation != null ? roleMappingResource.clientLevel(clientRepresentation.getId()).listAll() : roleMappingResource.realmLevel().listAll();
            List<RoleRepresentation> availableRoles = clientResource != null ? clientResource.roles().list() : realmResource.roles().list();
            List<RoleRepresentation> requestedRoles = availableRoles.stream().filter(role -> Arrays.stream(roles).anyMatch(name -> role.getName().equals(name))).collect(Collectors.toList());

            // Strip out requested roles that are already in a requested composite role
            List<String> removeRequestedRoles = requestedRoles.stream()
                .filter(RoleRepresentation::isComposite)
                .flatMap(role ->
                    realmResource.rolesById().getRoleComposites(role.getId()).stream().map(RoleRepresentation::getId)
                ).collect(Collectors.toList());

            requestedRoles = requestedRoles.stream()
                .filter(role -> removeRequestedRoles.stream().noneMatch(id -> id.equals(role.getId())))
                .collect(Collectors.toList());


            // Get newly defined roles
            List<RoleRepresentation> addRoles = requestedRoles.isEmpty() ? Collections.emptyList() : requestedRoles.stream()
                .filter(requestedRole -> existingRoles.stream().noneMatch(r -> r.getId().equals(requestedRole.getId())))
                .collect(Collectors.toList());

            // Remove obsolete roles
            List<RoleRepresentation> finalRequestedRoles = requestedRoles;
            List<RoleRepresentation> removeRoles = requestedRoles.isEmpty() ? existingRoles : existingRoles.stream()
                .filter(r -> finalRequestedRoles.stream().noneMatch(requestedRole -> requestedRole.getId().equals(r.getId())))
                .collect(Collectors.toList());

            if (!removeRoles.isEmpty()) {
                if (clientRepresentation != null) {
                    roleMappingResource.clientLevel(clientRepresentation.getId()).remove(removeRoles);
                } else {
                    roleMappingResource.realmLevel().remove(removeRoles);
                }
            }
            if (!addRoles.isEmpty()) {
                if (clientRepresentation != null) {
                    roleMappingResource.clientLevel(clientRepresentation.getId()).add(addRoles);
                } else {
                    roleMappingResource.realmLevel().add(addRoles);
                }
            }

            return null;
        });
    }

    @Override
    public boolean isMasterRealmAdmin(String userId) {
        Optional<UserRepresentation> adminUser = getRealms(realmsResource ->
            realmsResource.realm(MASTER_REALM)
                .users()
                .search(MASTER_REALM_ADMIN_USER, null, null))
            .stream()
            .filter(user -> user.getUsername().equals(MASTER_REALM_ADMIN_USER))
            .findFirst();

        if (!adminUser.isPresent()) {
            throw new IllegalStateException("Can't load master realm admin user");
        }
        return adminUser.map(UserRepresentation::getId).map(id -> id.equals(userId)).orElse(false);
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
            existing.setRegistrationAllowed(tenant.getRegistrationAllowed());
            existing.setRegistrationEmailAsUsername(tenant.getRegistrationEmailAsUsername());
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

            try {
                realmsResource.create(realmRepresentation);
                RealmResource realmResource = realmsResource.realm(tenant.getRealm());
                realmRepresentation = realmResource.toRepresentation();

                // Need a committed realmRepresentation to update the security
                configureRealm(realmRepresentation);
                realmResource.update(realmRepresentation);

                // Auto create the standard openremote client
                ClientRepresentation clientRepresentation = generateOpenRemoteClientRepresentation();
                createUpdateClient(tenant.getRealm(), clientRepresentation);

                // Add restricted realm role
                realmResource.roles().create(new RoleRepresentation(RESTRICTED_USER_REALM_ROLE, "Restricted access to assets", false));

                Tenant createdTenant = convert(realmRepresentation, Tenant.class);
                publishModification(PersistenceEvent.Cause.CREATE, createdTenant);
                return createdTenant;
            } catch (Exception e) {
                LOG.log(Level.INFO, "Failed to create tenant: " + tenant, e);
                throw e;
            }
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

    /**
     * Load keycloak proxy credentials from file system
     */
    public OAuthGrant loadCredentials() {
        // Try and load keycloak proxy credentials from file
        String grantFile = getString(container.getConfig(), KEYCLOAK_GRANT_FILE, KEYCLOAK_GRANT_FILE_DEFAULT);
        Path grantPath = TextUtil.isNullOrEmpty(grantFile) ? null : Paths.get(grantFile);
        OAuthGrant grant = null;

        if (grantPath != null && Files.isReadable(grantPath)) {
            LOG.info("Loading KEYCLOAK_GRANT_FILE: " + grantFile);

            try (InputStream is = Files.newInputStream(grantPath)) {
                String grantJson = IOUtils.toString(is, StandardCharsets.UTF_8);
                grant = ValueUtil.parse(grantJson, OAuthGrant.class).orElseGet(() -> {
                    LOG.info("Failed to load KEYCLOAK_GRANT_FILE: " + grantFile);
                    return null;
                });
            } catch (Exception ex) {
                throw new ExceptionInInitializerError(ex);
            }
        }
        return grant;
    }

    /**
     * Save Keycloak proxy credentials to the file system
     */
    public void saveCredentials(OAuthGrant grant) {
        String grantFile = getString(container.getConfig(), KEYCLOAK_GRANT_FILE, KEYCLOAK_GRANT_FILE_DEFAULT);

        if (TextUtil.isNullOrEmpty(grantFile)) {
            return;
        }
        Path grantPath = Paths.get(grantFile);

        try {
            Files.write(grantPath, ValueUtil.asJSON(grant).orElse("null").getBytes(),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            LOG.info("Failed to write KEYCLOAK_GRANT_FILE: " + grantFile);
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
        return getRealms(realmsResource ->
            withClientResource(realm, clientId, realmsResource, (clientRepresentation, clientResource) ->
                clientRepresentation, null));
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

        return getRealms(realmsResource ->
            withClientResource(realm, client.getClientId(), realmsResource, (clientRepresentation, clientResource) -> {
                clientResource.update(client);
                return client;
            },
        () -> {
            ClientsResource clientsResource = realmsResource.realm(realm).clients();
            Response response = clientsResource.create(client);
            response.close();
            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                LOG.info("Failed to create client response=" + response.getStatusInfo().getStatusCode() + ": " + client);
                return null;
            }

            ClientRepresentation newClient = clientsResource.findByClientId(client.getClientId()).get(0);
            ClientResource clientResource = clientsResource.get(newClient.getId());
            addDefaultRoles(clientResource.roles());
            return newClient;
        }));
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
            return withClientResource(realm, clientId, realmsResource, (clientRepresentation, clientResource) -> {
                clientResource.remove();
                return null;
            }, () -> {
                throw new NotFoundException("Delete client failed as client not found: " + clientId);
            });
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
    public boolean isRestrictedUser(AuthContext authContext) {

        return authContext.hasRealmRole(RESTRICTED_USER_REALM_ROLE);
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
        if (isRestrictedUser(auth))
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
