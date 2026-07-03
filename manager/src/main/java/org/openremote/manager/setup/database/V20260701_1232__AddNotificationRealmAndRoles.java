/*
 * Copyright 2026, OpenRemote Inc.
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
import java.sql.Statement;
import java.util.List;
import java.util.logging.Logger;

/**
 * Flyway migration that adds a {@code realm} column to the {@code NOTIFICATION} table (backfilling existing rows) and
 * adds the {@code read:notifications} / {@code write:notifications} client roles to the {@code openremote} client of
 * every realm.
 * <p>
 * The schema change runs as SQL on the migration's own database connection. The roles are created through the Keycloak
 * admin API rather than by writing to Keycloak's tables directly, so the migration doesn't couple to Keycloak's
 * internal schema. It authenticates with the stored manager credentials written to
 * {@code <OR_STORAGE_DIR>/manager/keycloak-credentials.json} (the {@code manager-keycloak} super-user that the identity
 * provider provisions on first startup) rather than the {@code OR_ADMIN_PASSWORD} admin credentials - the admin password
 * is only meant to be used once, at initial startup, to create those manager credentials.
 * <p>
 * When the credentials file is absent this is a clean install: the {@code openremote} clients don't exist yet at
 * migration time and their roles (including these) are created by {@code KeycloakInitSetup}, so the role step is
 * skipped. The schema change still runs. Only on an upgrade, where the file already exists, is there role work to do.
 * <p>
 * This is a Java (not {@code .sql}) migration because it needs the Keycloak admin API. Note the trade-off: it now
 * depends on Keycloak being reachable at migration time (whereas plain SQL only needed the database). See
 * {@code V20250916_01__AddServiceRoles} for the same credential/role handling.
 */
public class V20260701_1232__AddNotificationRealmAndRoles extends BaseJavaMigration {

    private static final Logger LOG = Logger.getLogger(V20260701_1232__AddNotificationRealmAndRoles.class.getName());

    // Adds and backfills the NOTIFICATION.realm column. NOTIFICATION/ASSET are unqualified so they resolve to the
    // manager schema (the connection's default schema); Keycloak's tables are in "public" so they're qualified, as the
    // migration connection's search_path may not include it.
    private static final String SCHEMA_SQL = """
        -- add the realm column (initially nullable so existing rows can be backfilled)
        ALTER TABLE NOTIFICATION ADD COLUMN realm VARCHAR(255);

        -- backfill the realm of existing notifications from their target, mirroring NotificationService.resolveTargetRealm:
        --   REALM target -> the target id is the realm name itself
        --   ASSET target -> the asset's realm
        --   USER  target -> the user's realm
        UPDATE NOTIFICATION
            SET realm = TARGET_ID
            WHERE realm IS NULL AND TARGET = 'REALM';

        UPDATE NOTIFICATION n
            SET realm = a.REALM
            FROM ASSET a
            WHERE n.realm IS NULL AND n.TARGET = 'ASSET' AND a.ID = n.TARGET_ID;

        UPDATE NOTIFICATION n
            SET realm = r.name
            FROM public.user_entity u
            JOIN public.realm r ON r.id = u.realm_id
            WHERE n.realm IS NULL AND n.TARGET = 'USER' AND u.id = n.TARGET_ID;

        -- any rows whose target could not be resolved (CUSTOM targets, or a since-deleted asset/user) fall back to the
        -- default realm, matching resolveTargetRealm's fallback, so the NOT NULL constraint can be applied
        UPDATE NOTIFICATION SET realm = 'master' WHERE realm IS NULL;

        -- enforce NOT NULL to match the entity (@NotNull / @Column(nullable = false))
        ALTER TABLE NOTIFICATION ALTER COLUMN realm SET NOT NULL;
        """;

    @Override
    public void migrate(Context context) throws Exception {

        // 1) Schema change on the migration's DB connection (transactional with the rest of this migration)
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute(SCHEMA_SQL);
        }

        // 2) Roles via the Keycloak admin API using the stored manager credentials
        OAuthPasswordGrant credentials = loadStoredCredentials();
        if (credentials == null) {
            // No stored credentials: clean install (roles are created by KeycloakInitSetup) or storage was wiped.
            LOG.info("No stored keycloak credentials found; skipping notification role backfill");
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

            // For every realm, ensure the openremote client has the read:notifications and write:notifications roles
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
                createRoleIfNotExists(clientRoles, "read:notifications", "Read notifications");
                createRoleIfNotExists(clientRoles, "write:notifications", "Write notification data");

                // Add the leaf roles to the existing "read"/"write" composites so users assigned only the broad
                // composites inherit them, matching ClientRole.READ / ClientRole.WRITE
                // (read -> read:notifications, write -> read:notifications + write:notifications).
                RoleRepresentation readNotif = clientRoles.get("read:notifications").toRepresentation();
                RoleRepresentation writeNotif = clientRoles.get("write:notifications").toRepresentation();
                addToComposite(clientRoles, "read", readNotif);
                addToComposite(clientRoles, "write", readNotif, writeNotif);
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

    // Add child roles to the named composite role; addComposites is idempotent so re-running is safe
    private void addToComposite(RolesResource roles, String compositeName, RoleRepresentation... children) {
        try {
            roles.get(compositeName).addComposites(List.of(children));
        } catch (NotFoundException e) {
            LOG.warning("Composite role '" + compositeName + "' not found; skipping composite wiring");
        }
    }
}
