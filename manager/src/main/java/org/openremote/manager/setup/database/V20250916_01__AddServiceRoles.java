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
import org.openremote.container.security.keycloak.KeycloakIdentityProvider;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;
import org.openremote.model.Constants;
import org.openremote.model.auth.OAuthGrant;
import org.openremote.model.auth.OAuthPasswordGrant;
import org.openremote.model.util.ValueUtil;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.UriBuilder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Logger;

/**
 * Flyway migration that adds the {@code read:services} and {@code write:services} client roles to the
 * {@code openremote} client of every realm, using the Keycloak admin API.
 * <p>
 * It authenticates with the stored manager credentials written to {@code <OR_STORAGE_DIR>/manager/keycloak-credentials.json}
 * (the {@code manager-keycloak} super-user that the identity provider provisions on first startup) rather than the
 * {@code OR_ADMIN_PASSWORD} admin credentials. The admin password is only meant to be used once, at initial startup, to
 * create those manager credentials; reusing the stored grant here keeps the migration working even when the admin
 * password isn't available (e.g. rotated or unset after bootstrap).
 * <p>
 * When the credentials file is absent this is a clean install: the {@code openremote} clients don't exist yet at
 * migration time and their roles (including these) are created by {@code KeycloakInitSetup}, so the migration simply
 * skips. It only has work to do on an upgrade, where the file already exists from a previous startup.
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

        OAuthPasswordGrant credentials = loadStoredCredentials();
        if (credentials == null) {
            // No stored credentials: clean install (roles are created by KeycloakInitSetup) or storage was wiped.
            LOG.info("No stored keycloak credentials found; skipping service role backfill");
            return;
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
                createRoleIfNotExists(clientRoles, "write:services", "Write service data");
                createRoleIfNotExists(clientRoles, "read:services", "View services");
            }
        }
    }

    /**
     * Loads the stored manager credentials the same way the identity provider does: the {@code OR_KEYCLOAK_GRANT_FILE}
     * (a JSON-serialised {@link OAuthGrant}) resolved relative to {@code OR_STORAGE_DIR}. Returns {@code null} if the
     * file is absent or doesn't hold a usable password grant.
     */
    private OAuthPasswordGrant loadStoredCredentials() {
        String storageDir = System.getenv().getOrDefault(
                PersistenceService.OR_STORAGE_DIR, PersistenceService.OR_STORAGE_DIR_DEFAULT);
        String grantFile = System.getenv().getOrDefault(
                ManagerKeycloakIdentityProvider.OR_KEYCLOAK_GRANT_FILE,
                ManagerKeycloakIdentityProvider.OR_KEYCLOAK_GRANT_FILE_DEFAULT);

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

    // Create the role if it doesn't exist by handling the NotFoundException
    private void createRoleIfNotExists(RolesResource roles, String roleName, String description) {
        try {
            roles.get(roleName).toRepresentation();
        } catch (NotFoundException e) {
            roles.create(new RoleRepresentation(roleName, description, false));
        }
    }

    // Talks to Keycloak over HTTP (not the migration's DB connection), so it can't run in a DB transaction
    @Override
    public boolean canExecuteInTransaction() {
        return false;
    }
}
