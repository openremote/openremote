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

import org.flywaydb.core.api.migration.Context;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.openremote.model.Constants;
import org.openremote.model.security.ClientRole;

import jakarta.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for Flyway migrations that add client roles to the {@code openremote} client of every realm and wire
 * them into the existing {@code read}/{@code write} composite roles (read → {@link #getReadRoles()},
 * write → {@link #getReadRoles()} + {@link #getWriteRoles()}). See {@link AbstractKeycloakMigration} for
 * credential handling, the identity-provider guard, and why subclasses must stay Java migrations.
 */
public abstract class AbstractKeycloakRolesMigration extends AbstractKeycloakMigration {

    /**
     * The leaf roles to create and wire into both the {@code read} and {@code write} composite roles.
     */
    protected abstract List<ClientRole> getReadRoles();

    /**
     * The leaf roles to create and wire into the {@code write} composite role only.
     */
    protected abstract List<ClientRole> getWriteRoles();

    @Override
    public void migrate(Context context) throws Exception {
        if (!isKeycloakDeployment()) {
            LOG.info("Identity provider is not keycloak; skipping role migration");
            return;
        }

        try (Keycloak keycloak = openKeycloak()) {
            List<String> realmNames = keycloak.realms().findAll().stream()
                    .map(RealmRepresentation::getRealm)
                    .toList();

            for (String realmName : realmNames) {
                RealmResource realm = keycloak.realm(realmName);

                List<ClientRepresentation> clients = realm.clients().findByClientId(Constants.KEYCLOAK_CLIENT_ID);
                if (clients.isEmpty()) {
                    LOG.warning("Client '" + Constants.KEYCLOAK_CLIENT_ID + "' not found in realm " + realmName
                            + ", skipping role creation.");
                    continue;
                }
                ClientResource clientResource = realm.clients().get(clients.get(0).getId());
                RolesResource clientRoles = clientResource.roles();

                List<RoleRepresentation> readComposites = new ArrayList<>();
                List<RoleRepresentation> writeComposites = new ArrayList<>();

                for (ClientRole role : getReadRoles()) {
                    createRoleIfNotFound(clientRoles, role);
                    RoleRepresentation representation = clientRoles.get(role.getValue()).toRepresentation();
                    readComposites.add(representation);
                    writeComposites.add(representation);
                }
                for (ClientRole role : getWriteRoles()) {
                    createRoleIfNotFound(clientRoles, role);
                    writeComposites.add(clientRoles.get(role.getValue()).toRepresentation());
                }

                addToComposite(clientRoles, ClientRole.READ.getValue(), readComposites);
                addToComposite(clientRoles, ClientRole.WRITE.getValue(), writeComposites);
            }
        }
    }

    private void createRoleIfNotFound(RolesResource roles, ClientRole role) {
        try {
            roles.get(role.getValue()).toRepresentation();
        } catch (NotFoundException e) {
            roles.create(new RoleRepresentation(role.getValue(), role.getDescription(), false));
        }
    }

    private void addToComposite(RolesResource roles, String compositeName, List<RoleRepresentation> children) {
        try {
            roles.get(compositeName).addComposites(children); // addComposites is idempotent
        } catch (NotFoundException e) {
            LOG.warning("Composite role '" + compositeName + "' not found; skipping composite wiring");
        }
    }
}
