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
import jakarta.persistence.Query;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.utils.URIBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.*;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.representations.idm.*;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.security.AuthContext;
import org.openremote.container.security.keycloak.KeycloakIdentityProvider;
import org.openremote.container.timer.TimerService;
import org.openremote.container.util.UniqueIdentifierGenerator;
import org.openremote.container.web.WebService;
import org.openremote.manager.apps.ConsoleAppService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.model.Constants;
import org.openremote.model.Container;
import org.openremote.model.PersistenceEvent;
import org.openremote.model.asset.Asset;
import org.openremote.model.auth.OAuthGrant;
import org.openremote.model.auth.OAuthPasswordGrant;
import org.openremote.model.event.shared.RealmFilter;
import org.openremote.model.gateway.GatewayConnection;
import org.openremote.model.provisioning.ProvisioningConfig;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.UserQuery;
import org.openremote.model.query.filter.RealmPredicate;
import org.openremote.model.rules.RealmRuleset;
import org.openremote.model.security.*;
import org.openremote.model.util.TextUtil;
import org.openremote.model.util.ValueUtil;

import java.io.InputStream;
import java.net.URISyntaxException;
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

import static org.openremote.container.util.MapAccess.*;
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
    public static final String OR_KEYCLOAK_GRANT_FILE = "OR_KEYCLOAK_GRANT_FILE";
    public static final String OR_KEYCLOAK_GRANT_FILE_DEFAULT = "manager/build/keycloak.json";
    public static final String OR_KEYCLOAK_PUBLIC_URI = "OR_KEYCLOAK_PUBLIC_URI";
    public static final String OR_KEYCLOAK_PUBLIC_URI_DEFAULT = "/auth";

    protected PersistenceService persistenceService;
    protected AssetStorageService assetStorageService;
    protected TimerService timerService;
    protected MessageBrokerService messageBrokerService;
    protected ClientEventService clientEventService;
    protected ConsoleAppService consoleAppService;
    protected String keycloakAdminPassword;
    protected Container container;
    protected String frontendURI;
    protected List<String> validRedirectUris;

    @Override
    public void init(Container container) {
        super.init(container);
        this.container = container;

        String keycloakPublicUri = getString(container.getConfig(), OR_KEYCLOAK_PUBLIC_URI, OR_KEYCLOAK_PUBLIC_URI_DEFAULT);
        try {
            URIBuilder uriBuilder = new URIBuilder(keycloakPublicUri);
            frontendURI = uriBuilder.build().toString();
        } catch (URISyntaxException e) {
            LOG.log(Level.SEVERE, "Failed to build Keycloak public URI", e);
            throw new RuntimeException(e);
        }

        keycloakAdminPassword = container.getConfig().getOrDefault(OR_ADMIN_PASSWORD, OR_ADMIN_PASSWORD_DEFAULT);
        timerService = container.getService(TimerService.class);
        persistenceService = container.getService(PersistenceService.class);
        messageBrokerService = container.getService(MessageBrokerService.class);
        clientEventService = container.getService(ClientEventService.class);
        consoleAppService = container.getService(ConsoleAppService.class);
        assetStorageService = container.getService(AssetStorageService.class);

        // Allow all external hostnames with wildcard and same host with wildcard
        validRedirectUris = new ArrayList<>();
        validRedirectUris.add("/*");
        validRedirectUris.addAll(WebService.getExternalHostnames(container).stream().map(host -> "https://" + host + "/*").toList());
    }

    @Override
    public void start(Container container) {
        super.start(container);
        if (container.isDevMode()) {
            String keycloakPath = getString(container.getConfig(), OR_KEYCLOAK_PATH, OR_KEYCLOAK_PATH_DEFAULT);
            enableAuthProxy(container.getService(WebService.class), keycloakPath);
        }
    }

    @Override
    protected OAuthGrant getStoredCredentials(Container container) {
        // Try and load keycloak proxy credentials from the file system
        String grantFile = getString(container.getConfig(), OR_KEYCLOAK_GRANT_FILE, OR_KEYCLOAK_GRANT_FILE_DEFAULT);
        Path grantPath = TextUtil.isNullOrEmpty(grantFile) ? null : Paths.get(grantFile);
        OAuthGrant grant = null;

        if (grantPath != null && Files.isReadable(grantPath)) {
            LOG.info("Loading OR_KEYCLOAK_GRANT_FILE: " + grantFile);

            try (InputStream is = Files.newInputStream(grantPath)) {
                String grantJson = IOUtils.toString(is, StandardCharsets.UTF_8);
                grant = ValueUtil.parse(grantJson, OAuthGrant.class).orElseGet(() -> {
                    LOG.warning("Failed to load OR_KEYCLOAK_GRANT_FILE: " + grantFile);
                    return null;
                });
            } catch (Exception ex) {
                throw new ExceptionInInitializerError(ex);
            }
        }
        return grant;
    }

    @Override
    protected OAuthGrant generateStoredCredentials(Container container) {
        String grantFile = getString(container.getConfig(), OR_KEYCLOAK_GRANT_FILE, OR_KEYCLOAK_GRANT_FILE_DEFAULT);

        if (TextUtil.isNullOrEmpty(grantFile)) {
            return null;
        }

        // Create a new super user for the keycloak proxy so admin user can be modified if desired
        User keycloakProxyUser = new User()
            .setUsername(MANAGER_CLIENT_ID)
            .setEnabled(true)
            .setSystemAccount(true);
        String password = UniqueIdentifierGenerator.generateId();
        keycloakProxyUser = createUpdateUser(MASTER_REALM, keycloakProxyUser, password, true);

        // Make this proxy user a super user by giving them admin realm role
        updateUserRealmRoles(MASTER_REALM, keycloakProxyUser.getId(), addRealmRoles(MASTER_REALM, keycloakProxyUser.getId(), REALM_ADMIN_ROLE));

        // Use same details as default keycloak grant but change the username and password to our new user
        OAuthPasswordGrant grant = getDefaultKeycloakGrant(container);
        grant.setUsername(keycloakProxyUser.getUsername()).setPassword(password);
        Path grantPath = Paths.get(grantFile);

        try {
            Files.write(grantPath, ValueUtil.asJSON(grant).orElse("null").getBytes(),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            LOG.info("Failed to write " + OR_KEYCLOAK_GRANT_FILE + ": " + grantFile);
            return null;
        }

        return grant;
    }

    @Override
    protected void addClientRedirectUris(String client, List<String> redirectUrls, boolean devMode) {
        // Callback URL used by Manager web client authentication, any relative path to "ourselves" is fine
        String realmManagerCallbackUrl = UriBuilder.fromUri("/").path(client).path("*").build().toString();
        redirectUrls.add(realmManagerCallbackUrl);
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
    public User[] queryUsers(UserQuery userQuery) {
        return ManagerIdentityProvider.getUsersFromDb(persistenceService, userQuery);
    }

    @Override
    public User getUser(String userId) {
        return ManagerIdentityProvider.getUserByIdFromDb(persistenceService, userId);
    }

    @Override
    public User getUserByUsername(String realm, String username) {
        return ManagerIdentityProvider.getUserByUsernameFromDb(persistenceService, realm, username);
    }

    @Override
    public User createUpdateUser(String realm, final User user, String passwordSecret, boolean allowUpdate) throws WebApplicationException {
        return getRealms(realmsResource -> {

            // Force lowercase username
            if (user.getUsername() != null) {
                user.setUsername(user.getUsername().toLowerCase(Locale.ROOT));
            }

            if (!user.isServiceAccount() && allowUpdate) {
                if (getRealm(realm).getRegistrationEmailAsUsername() ? user.getEmail() == null : user.getUsername() == null) {
                    throw new BadRequestException("Attempt to create/update user but no username or email provided: User=" + user);
                }
            }

            boolean isUpdate = false;
            User existingUser = user.getId() != null ? getUser(user.getId()) : getUserByUsername(realm, user.getUsername());
            ClientRepresentation clientRepresentation;
            UserRepresentation userRepresentation;

            if(existingUser != null && !allowUpdate) {
                String msg = "Attempt to create user but it already exists: User=" + user;
                LOG.warning(msg);
                throw new ForbiddenException(msg);
            }

            if (existingUser == null && user.isServiceAccount()) {
                // Could be a service user
                userRepresentation = withClientResource(realm, user.getUsername(), realmsResource, (clientRep, clientResource) -> {
                    UserRepresentation userRep = clientResource.getServiceAccountUser();
                    if (userRep == null) {
                        String msg = "Attempt to update/create service user but a regular client with same client ID as this username already exists: User=" + user;
                        LOG.warning(msg);
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
                LOG.warning(msg);
                throw new BadRequestException(msg);
            }

            if (existingUser != null) {
                isUpdate = true;

                if (existingUser.isServiceAccount() != user.isServiceAccount()) {
                    String msg = "Attempt to update user service account flag not allowed: User=" + user;
                    LOG.warning(msg);
                    throw new NotAllowedException(msg);
                }

                if (existingUser.isServiceAccount() && !existingUser.getUsername().equals(user.getUsername())) {
                    String msg = "Attempt to update username of service user not allowed: User=" + user;
                    LOG.warning(msg);
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
                userRepresentation.setAttributes(user.getAttributeMap());
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

            User updatedUser = convert(userRepresentation, User.class);
            if (updatedUser != null) {
                updatedUser.setRealm(realm);
                if (updatedUser.isServiceAccount()) {
                    updatedUser.setSecret(passwordSecret);
                }

                if (existingUser != null) {
                    // Push realm ID into updated user
                    updatedUser.setRealmId(existingUser.getRealmId());
                }
            }

            persistenceService.publishPersistenceEvent(
                (isUpdate ? PersistenceEvent.Cause.UPDATE : PersistenceEvent.Cause.CREATE),
                updatedUser,
                existingUser,
                User.class,
                Collections.singletonList("attributes"),
                null);

            return updatedUser;
        });
    }

    @Override
    public void deleteUser(String realm, String userId) {

        User user = getUser(userId);

        if (user == null) {
            return;
        }

        if (user.getUsername().equals(MASTER_REALM_ADMIN_USER) && user.getRealm().equals(MASTER_REALM)) {
            throw new IllegalStateException("Cannot delete master realm admin user");
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

        persistenceService.publishPersistenceEvent(PersistenceEvent.Cause.DELETE,
            null,
            user,
            User.class,
            Collections.singletonList("attributes"),
            null);
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
            }, () -> new Role[0]);
        });
    }

    @Override
    public Role[] getUserRealmRoles(String realm, String userId) {
        return getRealms(realmsResource -> {
            RealmResource realmResource = realmsResource.realm(realm);
            RoleMappingResource roleMappingResource = realmResource.users().get(userId).roles();

                RolesResource rolesResource = realmResource.roles();
                List<RoleRepresentation> allRoles = rolesResource.list();
                List<RoleRepresentation> effectiveRoles = roleMappingResource.realmLevel().listEffective();

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
    public void updateUserRoles(@NotNull String realm, @NotNull String userId, @NotNull String client, String...roles) {
        getRealms(realmsResource -> {
            RealmResource realmResource = realmsResource.realm(realm);
            UserRepresentation user = realmResource.users().get(userId).toRepresentation();

            if (user == null) {
                throw new IllegalStateException("Multiple users with the same username found");
            }

            RoleMappingResource roleMappingResource = realmResource.users().get(user.getId()).roles();
            ClientRepresentation clientRepresentation = getClient(realm, client);

            if (clientRepresentation == null) {
                throw new IllegalStateException("Invalid client: " + client);
            }

            ClientResource clientResource = realmResource.clients().get(clientRepresentation.getId());

            // Get all roles
            List<RoleRepresentation> existingRoles = roleMappingResource.clientLevel(clientRepresentation.getId()).listAll();
            List<RoleRepresentation> availableRoles = clientResource.roles().list();
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
                roleMappingResource.clientLevel(clientRepresentation.getId()).remove(removeRoles);
            }
            if (!addRoles.isEmpty()) {
                roleMappingResource.clientLevel(clientRepresentation.getId()).add(addRoles);
            }

            return null;
        });
    }

    @Override
    public void updateUserRealmRoles(String realm, String userId, String... roles) {
        getRealms(realmsResource -> {
            RealmResource realmResource = realmsResource.realm(realm);
            UserRepresentation user = realmResource.users().get(userId).toRepresentation();

            if (user == null) {
                throw new IllegalStateException("Multiple users with the same username found");
            }

            RoleMappingResource roleMappingResource = realmResource.users().get(user.getId()).roles();

            // Get all roles
            List<RoleRepresentation> existingRoles = roleMappingResource.realmLevel().listAll();
            List<RoleRepresentation> availableRoles = realmResource.roles().list();
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
                roleMappingResource.realmLevel().remove(removeRoles);
            }
            if (!addRoles.isEmpty()) {
                roleMappingResource.realmLevel().add(addRoles);
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
    public Realm[] getRealms() {
        return ManagerIdentityProvider.getRealmsFromDb(persistenceService);
    }

    @Override
    public Realm getRealm(String realm) {
        try {
            return ManagerIdentityProvider.getRealmFromDb(persistenceService, realm);
        } catch (Exception ex) {
            LOG.log(Level.INFO, "Failed to get realm by name: " + realm, ex);
        }
        return null;
    }

    @Override
    public void updateRealm(Realm realm) {
        LOG.fine("Update realm: " + realm);
        getRealms(realmsResource -> {

            if (TextUtil.isNullOrEmpty(realm.getId())) {
                throw new IllegalStateException("Realm must already exist, ID does not match an existing realm");
            }

            // Force realm to lowercase
            realm.setName(realm.getName().toLowerCase(Locale.ROOT));

            // Find existing realm by ID as realm name could have been changed
            RealmRepresentation realmRepresentation = realmsResource.findAll().stream().filter(r -> r.getId().equals(realm.getId())).findFirst().orElse(null);

            if (realmRepresentation == null) {
                throw new IllegalStateException("Realm must already exist, ID does not match an existing realm");
            }

            String realmName = realmRepresentation.getRealm();
            RealmResource realmResource = realmsResource.realm(realmName);
            Realm existingRealm = getRealm(realmName);

            // Realm only has a subset of realm representation so overlay on actual realm representation
            realmRepresentation.setDisplayName(realm.getDisplayName());
            realmRepresentation.setAccountTheme(realm.getAccountTheme());
            realmRepresentation.setAdminTheme(realm.getAdminTheme());
            realmRepresentation.setEmailTheme(realm.getEmailTheme());
            realmRepresentation.setLoginTheme(realm.getLoginTheme());
            realmRepresentation.setRememberMe(realm.getRememberMe());
            realmRepresentation.setVerifyEmail(realm.getVerifyEmail());
            realmRepresentation.setLoginWithEmailAllowed(realm.getLoginWithEmail());
            realmRepresentation.setRegistrationAllowed(realm.getRegistrationAllowed());
            realmRepresentation.setRegistrationEmailAsUsername(realm.getRegistrationEmailAsUsername());
            realmRepresentation.setEnabled(realm.getEnabled());
            realmRepresentation.setDuplicateEmailsAllowed(realm.getDuplicateEmailsAllowed());
            realmRepresentation.setResetPasswordAllowed(realm.getResetPasswordAllowed());
            realmRepresentation.setNotBefore(realm.getNotBefore() != null ? realm.getNotBefore().intValue() : null);
            configureRealm(realmRepresentation);
            realmResource.update(realmRepresentation);

            Set<RealmRole> existingRealmRoles = existingRealm.getRealmRoles();
            existingRealm.setRealmRoles(existingRealmRoles);

            // Update realm roles if required
            if (realm.getRealmRoles() != null) {

                Set<RealmRole> realmRoles = realm.getNormalisedRealmRoles();
                RolesResource rolesResource = realmResource.roles();

                // Handle removed roles
                existingRealmRoles.stream().filter(realmRole -> !realmRoles.contains(realmRole)).forEach(realmRole -> {
                    LOG.finest("Removing realm role + " + realmRole);
                    rolesResource.deleteRole(realmRole.getName());
                });
                // Handle added roles
                realmRoles.stream().filter(realmRole -> !existingRealmRoles.contains(realmRole)).forEach(realmRole -> {
                    LOG.finest("Adding realm role + " + realmRole);
                    rolesResource.create(new RoleRepresentation(realmRole.getName(), realmRole.getDescription(), false));
                });
            }

            Realm updatedRealm = convert(realmRepresentation, Realm.class);
            updatedRealm.setName(realmRepresentation.getRealm());
            updatedRealm.setRealmRoles((realm.getRealmRoles() == null) ? existingRealmRoles : realm.getNormalisedRealmRoles());
            persistenceService.publishPersistenceEvent(PersistenceEvent.Cause.UPDATE, updatedRealm, existingRealm, Realm.class, null, null);
            return null;
        });
    }

    @Override
    public Realm createRealm(Realm realm) {
        LOG.fine("Create realm: " + realm);
        return getRealms(realmsResource -> {

            // Force realm to lowercase
            realm.setName(realm.getName().toLowerCase(Locale.ROOT));

            RealmRepresentation realmRepresentation = convert(realm, RealmRepresentation.class);
            // Inject name as it is called realm in the realmRepresentation
            realmRepresentation.setRealm(realm.getName());

            try {
                realmsResource.create(realmRepresentation);
                RealmResource realmResource = realmsResource.realm(realm.getName());
                realmRepresentation = realmResource.toRepresentation();

                // Need a committed realmRepresentation to update the security
                configureRealm(realmRepresentation);
                realmResource.update(realmRepresentation);

                // Set realm roles
                RolesResource rolesResource = realmResource.roles();
                List<RoleRepresentation> existingRealmRoles = rolesResource.list();
                realm.getNormalisedRealmRoles().stream().filter(realmRole -> existingRealmRoles.stream().noneMatch(roleRepresentation -> roleRepresentation.getName().equals(realmRole.getName())))
                .forEach(realmRole -> {
                    LOG.finest("Adding realm role + " + realmRole);
                    rolesResource.create(new RoleRepresentation(realmRole.getName(), realmRole.getDescription(), false));
                });

                // Auto create the standard openremote client
                ClientRepresentation clientRepresentation = generateOpenRemoteClientRepresentation();
                createUpdateClient(realm.getName(), clientRepresentation);

                Realm createdRealm = convert(realmRepresentation, Realm.class);
                createdRealm.setName(realmRepresentation.getRealm());
                createdRealm.setRealmRoles(realm.getRealmRoles());
                persistenceService.publishPersistenceEvent(PersistenceEvent.Cause.CREATE, realm, null, Realm.class, null, null);
                return createdRealm;
            } catch (Exception e) {
                LOG.log(Level.INFO, "Failed to create realm: " + realm, e);
                throw e;
            }
        });
    }

    @Override
    public void deleteRealm(String realmName) {
        Realm realm = getRealm(realmName);

        if (realm == null) {
            throw new NotFoundException("Realm does not exist: " + realmName);
        }

        persistenceService.doTransaction(entityManager -> {

            // Delete gateway connections
            Query query = entityManager.createQuery("delete from " + GatewayConnection.class.getSimpleName() + " gc " +
                "where gc.localRealm = ?1");

            query.setParameter(1, realmName);
            query.executeUpdate();

            // Delete provisioning configs
            query = entityManager.createQuery("delete from " + ProvisioningConfig.class.getSimpleName() + " pc " +
                "where pc.realm = ?1");

            query.setParameter(1, realmName);
            query.executeUpdate();

            // Delete Rules
            query = entityManager.createQuery("delete from " + RealmRuleset.class.getSimpleName() + " rs " +
                "where rs.realm = ?1");
            query.setParameter(1, realmName);
            query.executeUpdate();

            // Delete Assets
            List<String> assetIds = assetStorageService.findAll(new AssetQuery().select(new AssetQuery.Select().excludeAttributes()).realm(new RealmPredicate(realmName))).stream().map(Asset::getId).toList();
            assetStorageService.delete(assetIds);
        });

        LOG.fine("Deleting realm: " + realmName);
        getRealms(realmsResource -> {
            realmsResource.realm(realmName).remove();
            return null;
        });
        persistenceService.publishPersistenceEvent(PersistenceEvent.Cause.DELETE, null, realm, Realm.class, null, null);
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
            client.setWebOrigins(Collections.singletonList("+"));
            client.setRedirectUris(validRedirectUris);
        }

        return client;
    }

    // TODO: Provide an implementation agnostic client
    public ClientRepresentation getClient(String realm, String client) {
        return getRealms(realmsResource ->
            withClientResource(realm, client, realmsResource, (clientRepresentation, clientResource) ->
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
                LOG.fine("Failed to create client response=" + response.getStatusInfo().getStatusCode() + ": " + client);
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
                LOG.fine("Invalid realm provided for deleteClient call: " + realm);
                return null;
            }

            LOG.fine("Deleting client: realm=" + realm + ", client ID=" + clientId);
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
     * in the same realm as the realm and the realm is active.
     */
    @Override
    public boolean isRealmActiveAndAccessible(AuthContext authContext, Realm realm) {
        if (realm == null) {
            return false;
        }

        boolean isSuperUser = authContext != null && authContext.isSuperUser();
        boolean isUsersRealm = isSuperUser || authContext == null || authContext.isRealmAccessibleByUser(realm.getName());

        return isSuperUser || (isUsersRealm && realm.isActive(timerService.getCurrentTimeMillis()));
    }

    /**
     * @return <code>true</code> if the user is the superuser (admin) or if the user is authenticated
     * in the same realm and the realm is active.
     */
    @Override
    public boolean isRealmActiveAndAccessible(AuthContext authContext, String realm) {
        return isRealmActiveAndAccessible(authContext, getRealm(realm));
    }

    @Override
    public boolean realmExists(String realm) {
        return ManagerIdentityProvider.realmExistsFromDb(persistenceService, realm);
    }

    @Override
    public boolean isRestrictedUser(AuthContext authContext) {
        return authContext != null && authContext.hasRealmRole(RESTRICTED_USER_REALM_ROLE);
    }

    @Override
    public boolean isUserInRealm(String userId, String realm) {
        return ManagerIdentityProvider.userInRealmFromDb(persistenceService, userId, realm);
    }

    @Override
    public boolean canSubscribeWith(AuthContext auth, RealmFilter<?> filter, ClientRole... requiredRoles) {
        // Superuser can always subscribe
        if (auth.isSuperUser())
            return true;

        // Restricted users get nothing
        if (isRestrictedUser(auth))
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
            String authenticatedRealm = auth.getAuthenticatedRealmName();

            if (TextUtil.isNullOrEmpty(authenticatedRealm))
                return false;
            if (authenticatedRealm.equals(filter.getName()))
                return true;
        }

        return false;
    }

    @Override
    public String getFrontendURI() {
        return frontendURI;
    }

    protected void configureRealm(RealmRepresentation realmRepresentation) {

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
        String host = container.getConfig().getOrDefault(OR_EMAIL_HOST, null);
        if (!TextUtil.isNullOrEmpty(host) && (realmRepresentation.getSmtpServer() == null || realmRepresentation.getSmtpServer().isEmpty())) {
            LOG.info("Configuring Keycloak SMTP settings for realm: " + realmRepresentation.getRealm());
            Map<String, String> emailConfig = new HashMap<>();
            emailConfig.put("host", host);
            emailConfig.put("port", container.getConfig().getOrDefault(OR_EMAIL_PORT, Integer.toString(OR_EMAIL_PORT_DEFAULT)));
            emailConfig.put("user", container.getConfig().getOrDefault(OR_EMAIL_USER, null));
            emailConfig.put("password", container.getConfig().getOrDefault(OR_EMAIL_PASSWORD, null));
            emailConfig.put("auth", container.getConfig().containsKey(OR_EMAIL_USER) ? "true" : "false");
            emailConfig.put("tls", Boolean.toString(getBoolean(container.getConfig(), OR_EMAIL_TLS, OR_EMAIL_TLS_DEFAULT)));
            emailConfig.put("ssl", Boolean.toString(!getBoolean(container.getConfig(), OR_EMAIL_TLS, OR_EMAIL_TLS_DEFAULT) && getString(container.getConfig(), OR_EMAIL_PROTOCOL, OR_EMAIL_PROTOCOL_DEFAULT).equals("smtps")));
            emailConfig.put("from", getString(container.getConfig(), OR_EMAIL_FROM, OR_EMAIL_FROM_DEFAULT));
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
            String allowedOriginsStr = String.join(" ", WebService.getAllowedOrigins(container));
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
