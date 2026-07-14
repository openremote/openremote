/*
 * Copyright 2025, OpenRemote Inc.
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
package org.openremote.manager.setup.database;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.security.IdentityProvider;
import org.openremote.container.security.keycloak.KeycloakIdentityProvider;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;
import org.openremote.model.Constants;
import org.openremote.model.auth.OAuthGrant;
import org.openremote.model.auth.OAuthPasswordGrant;
import org.openremote.model.security.ClientRole;
import org.openremote.model.util.ValueUtil;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.UriBuilder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.logging.Logger;

/**
 * Flyway migration that adds the {@code read:services} and {@code write:services} client roles to the
 * {@code openremote} client of every realm, using the Keycloak admin API, and wires them into the existing
 * {@code read}/{@code write} composite roles (matching ClientRole.READ / ClientRole.WRITE:
 * read -> read:services, write -> read:services + write:services).
 * <p>
 * It authenticates with the stored manager credentials written to {@code <OR_STORAGE_DIR>/manager/keycloak-credentials.json}
 * (the {@code manager-keycloak} super-user that the identity provider provisions on first startup) rather than the
 * {@code OR_ADMIN_PASSWORD} admin credentials. The admin password is only meant to be used once, at initial startup, to
 * create those manager credentials; reusing the stored grant here keeps the migration working even when the admin
 * password isn't available (e.g. rotated or unset after bootstrap).
 * <p>
 * On a clean install the migration skips entirely; {@code KeycloakInitSetup} will create the roles after Flyway.
 * On an upgrade, if the stored credentials are unavailable, it falls back to {@code OR_ADMIN_PASSWORD} only when
 * that is explicitly set, so a rotated/unset admin password doesn't silently leave the roles uncreated. If neither
 * credential source is available the migration fails rather than recording itself as applied without doing the work.
 * <p>
 * This must stay a Java migration and not be converted to a {@code .sql} file: Flyway records a {@code BaseJavaMigration}
 * as type JDBC with a null checksum, whereas a {@code .sql} file for the same version is type SQL with a real checksum,
 * so converting it would fail validation on any database that already applied this (released) version. The body may be
 * edited freely - Java migrations have no checksum, so changes here don't affect already-migrated databases.
 */
public class V20250916_01__AddServiceRoles extends BaseJavaMigration {

    private static final Logger LOG = Logger.getLogger(V20250916_01__AddServiceRoles.class.getName());

    @Override
    public void migrate(Context context) throws Exception {

        // On a clean install KeycloakInitSetup will add the roles after Flyway completes.
        if (isCleanInstall(context)) {
            LOG.info("Clean install detected; skipping service role migration "
                    + "(KeycloakInitSetup will create read:services and write:services roles on first startup)");
            return;
        }

        // Upgrade path: resolve credentials to authenticate against Keycloak.
        OAuthPasswordGrant credentials = loadStoredCredentials();
        if (credentials == null) {
            // Stored grant file is absent or unreadable; fall back to OR_ADMIN_PASSWORD only when explicitly set
            // so a rotated/unset admin password doesn't silently skip the migration.
            credentials = loadAdminFallbackCredentials();
            if (credentials == null) {
                throw new RuntimeException(
                        "Cannot add service roles: stored keycloak credentials are missing/unreadable and "
                                + IdentityProvider.OR_ADMIN_PASSWORD + " is not explicitly set. Either mount the "
                                + "storage volume containing <OR_STORAGE_DIR>/"
                                + ManagerKeycloakIdentityProvider.OR_KEYCLOAK_GRANT_FILE_DEFAULT
                                + " or set " + IdentityProvider.OR_ADMIN_PASSWORD
                                + " to the current admin password so the migration can authenticate against Keycloak.");
            }
            LOG.warning("Stored keycloak credentials not available; falling back to "
                    + IdentityProvider.OR_ADMIN_PASSWORD + " to add service roles");
        }

        String keycloakUrl = buildKeycloakUrl();

        try (Keycloak keycloak = Keycloak.getInstance(
                keycloakUrl,
                Constants.MASTER_REALM,
                credentials.getUsername(),
                credentials.getPassword(),
                KeycloakIdentityProvider.ADMIN_CLI_CLIENT_ID)) {

            List<String> realmNames = keycloak.realms().findAll().stream()
                    .map(RealmRepresentation::getRealm)
                    .toList();

            // For every realm, ensure the openremote client has the read:services and write:services roles
            for (String realmName : realmNames) {
                RealmResource realm = keycloak.realm(realmName);

                List<ClientRepresentation> clients = realm.clients().findByClientId(Constants.KEYCLOAK_CLIENT_ID);

                if (clients.isEmpty()) {
                    LOG.warning("Client '" + Constants.KEYCLOAK_CLIENT_ID + "' not found in realm " + realmName
                            + ", skipping role creation.");
                    continue; // Skip realms without the openremote client
                }
                ClientResource clientResource = realm.clients().get(clients.get(0).getId());
                RolesResource clientRoles = clientResource.roles();
                createRoleIfNotFound(clientRoles, ClientRole.WRITE_SERVICES);
                createRoleIfNotFound(clientRoles, ClientRole.READ_SERVICES);

                RoleRepresentation readServices = clientRoles.get(ClientRole.READ_SERVICES.getValue()).toRepresentation();
                RoleRepresentation writeServices = clientRoles.get(ClientRole.WRITE_SERVICES.getValue()).toRepresentation();
                addToComposite(clientRoles, ClientRole.READ.getValue(), readServices);
                addToComposite(clientRoles, ClientRole.WRITE.getValue(), readServices, writeServices);
            }
        }
    }

    /**
     * Loads the stored manager credentials, similar to {@link ManagerKeycloakIdentityProvider}: it reads
     * {@code OR_KEYCLOAK_GRANT_FILE} (a JSON-serialised {@link OAuthGrant}) resolved relative to {@code OR_STORAGE_DIR}.
     * Returns {@code null} if the file is absent or doesn't hold a usable password grant.
     */
    private OAuthPasswordGrant loadStoredCredentials() {
        String storageDir = System.getenv().getOrDefault(
                PersistenceService.OR_STORAGE_DIR, PersistenceService.OR_STORAGE_DIR_DEFAULT);
        String grantFile = System.getenv().getOrDefault(
                ManagerKeycloakIdentityProvider.OR_KEYCLOAK_GRANT_FILE,
                ManagerKeycloakIdentityProvider.OR_KEYCLOAK_GRANT_FILE_DEFAULT);

        if (grantFile == null || grantFile.isBlank()) {
            return null;
        }

        Path grantPath = Paths.get(storageDir).resolve(grantFile);
        if (!Files.isReadable(grantPath)) {
            return null;
        }

        try {
            String grantJson = Files.readString(grantPath, StandardCharsets.UTF_8);
            OAuthGrant grant = ValueUtil.parse(grantJson, OAuthGrant.class).orElse(null);
            if (grant instanceof OAuthPasswordGrant passwordGrant) {
                LOG.info("Loaded stored keycloak credentials from: " + grantPath);
                return passwordGrant;
            }
            LOG.warning("Stored keycloak credentials at " + grantPath + " are not a password grant; skipping");
        } catch (Exception ex) {
            LOG.warning("Failed to read stored keycloak credentials at " + grantPath + ": " + ex.getMessage());
        }
        return null;
    }

    /**
     * Builds an admin-password grant from {@link IdentityProvider#OR_ADMIN_PASSWORD}, but only when that env var is
     * explicitly set. Returns {@code null} when it isn't, so the caller can fail the migration rather than silently
     * using the default {@code "secret"} (which is wrong on any deployment that changed the admin password at
     * bootstrap).
     */
    private OAuthPasswordGrant loadAdminFallbackCredentials() {
        String adminPassword = System.getenv(IdentityProvider.OR_ADMIN_PASSWORD);
        if (adminPassword == null || adminPassword.isBlank()) {
            return null;
        }
        return new OAuthPasswordGrant(
                null,
                KeycloakIdentityProvider.ADMIN_CLI_CLIENT_ID,
                null,
                "openid",
                Constants.MASTER_REALM_ADMIN_USER,
                adminPassword);
    }

    /**
     * Heuristic for a clean install based on whether the {@code openremote} client exists in Keycloak's
     * {@code public.client} table:
     * <ul>
     *   <li>No row: clean install — {@code KeycloakInitSetup} hasn't run yet (it runs after Flyway).</li>
     *   <li>Row present: upgrade — the client was created by a previous startup.</li>
     *   <li>Table missing (SQLSTATE 42P01): clean install — Keycloak schema not initialised yet.</li>
     *   <li>Any other query failure: assume upgrade, so a transient error can't silently skip the migration.</li>
     * </ul>
     * {@code PUBLIC.REALM} is not used because a freshly initialised Keycloak always contains the {@code master}
     * realm, so that table is never empty even on a clean install.
     */
    private boolean isCleanInstall(Context context) {
        try (Statement statement = context.getConnection().createStatement();
                ResultSet resultSet = statement.executeQuery(
                        "SELECT COUNT(*) FROM public.client WHERE client_id = '" + Constants.KEYCLOAK_CLIENT_ID + "'")) {
            resultSet.next();
            return resultSet.getInt(1) == 0;
        } catch (SQLException ex) {
            if ("42P01".equals(ex.getSQLState())) { // undefined_table: Keycloak schema not created yet
                LOG.info("Keycloak client table does not exist; assuming clean install");
                return true;
            }
            LOG.warning("Could not query Keycloak client table: " + ex.getMessage() + "; assuming upgrade");
            return false;
        }
    }

    private String buildKeycloakUrl() {
        UriBuilder uriBuilder = UriBuilder.fromPath("/")
                .scheme("http")
                .host(System.getenv().getOrDefault(KeycloakIdentityProvider.OR_KEYCLOAK_HOST,
                        KeycloakIdentityProvider.OR_KEYCLOAK_HOST_DEFAULT))
                .port(Integer.parseInt(System.getenv().getOrDefault(KeycloakIdentityProvider.OR_KEYCLOAK_PORT,
                        String.valueOf(KeycloakIdentityProvider.OR_KEYCLOAK_PORT_DEFAULT))));

        String path = System.getenv().getOrDefault(KeycloakIdentityProvider.OR_KEYCLOAK_PATH,
                KeycloakIdentityProvider.OR_KEYCLOAK_PATH_DEFAULT);

        if (path != null && !path.isBlank()) {
            uriBuilder.path(path);
        }

        return uriBuilder.build().toString();
    }

    private void createRoleIfNotFound(RolesResource roles, ClientRole role) {
        try {
            roles.get(role.getValue()).toRepresentation();
        } catch (NotFoundException e) {
            roles.create(new RoleRepresentation(role.getValue(), role.getDescription(), false));
        }
    }

    private void addToComposite(RolesResource roles, String compositeName, RoleRepresentation... children) {
        try {
            roles.get(compositeName).addComposites(List.of(children)); // addComposites is idempotent
        } catch (NotFoundException e) {
            LOG.warning("Composite role '" + compositeName + "' not found; skipping composite wiring");
        }
    }

    // Talks to Keycloak over HTTP (not the migration's DB connection), so it can't run in a DB transaction
    @Override
    public boolean canExecuteInTransaction() {
        return false;
    }
}
