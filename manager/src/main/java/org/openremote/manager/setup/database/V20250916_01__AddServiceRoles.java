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
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.openremote.container.security.keycloak.KeycloakIdentityProvider;
import org.openremote.container.security.IdentityProvider;
import org.openremote.model.Constants;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.UriBuilder;

import java.util.List;
import java.util.logging.Logger;

/**
 * Flyway migration script to add write:services and read:services roles using
 * Keycloak
 * admin API.
 */
public class V20250916_01__AddServiceRoles extends BaseJavaMigration {

    private static final Logger LOG = Logger.getLogger(V20250916_01__AddServiceRoles.class.getName());

    @Override
    public void migrate(Context context) throws Exception {

        // Get the Keycloak port value from system properties for tests
        int keycloakPort = Integer.parseInt(System.getProperty(
                KeycloakIdentityProvider.OR_KEYCLOAK_PORT,
                System.getenv().getOrDefault(
                        KeycloakIdentityProvider.OR_KEYCLOAK_PORT,
                        String.valueOf(KeycloakIdentityProvider.OR_KEYCLOAK_PORT_DEFAULT)
                )
        ));

        // Build the keycloak URL
        UriBuilder uriBuilder = UriBuilder.fromPath("/")
                .scheme("http")
                .host(System.getenv().getOrDefault(KeycloakIdentityProvider.OR_KEYCLOAK_HOST,
                        KeycloakIdentityProvider.OR_KEYCLOAK_HOST_DEFAULT))
                .port(keycloakPort);

        String path = System.getenv().getOrDefault(KeycloakIdentityProvider.OR_KEYCLOAK_PATH,
                KeycloakIdentityProvider.OR_KEYCLOAK_PATH_DEFAULT);

        if (path != null && !path.isBlank()) {
            uriBuilder.path(path);
        }

        String keycloakUrl = uriBuilder.build().toString();
        String adminUser = Constants.MASTER_REALM_ADMIN_USER;
        String adminPassword = System.getenv().getOrDefault(IdentityProvider.OR_ADMIN_PASSWORD,
                IdentityProvider.OR_ADMIN_PASSWORD_DEFAULT);

        // Get the keycloak instance
        try (Keycloak keycloak = Keycloak.getInstance(
                keycloakUrl,
                Constants.MASTER_REALM,
                adminUser,
                adminPassword,
                KeycloakIdentityProvider.ADMIN_CLI_CLIENT_ID)) {

            // Get all available realms
            List<String> realmNames = keycloak.realms().findAll().stream()
                    .map(RealmRepresentation::getRealm)
                    .toList();

            // For every realm, check if the openremote client has the write:services and
            // read:services roles added
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

    // Create the role if it doesn't exist by handling the NotFoundException
    private void createRoleIfNotExists(RolesResource roles, String roleName, String description) {
        try {
            roles.get(roleName).toRepresentation();
        } catch (NotFoundException e) {
            roles.create(new RoleRepresentation(roleName, description, false));
        }
    }

    // Since its a code migration, no DB transaction is needed
    @Override
    public boolean canExecuteInTransaction() {
        return false;
    }
}
