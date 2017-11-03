/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.manager.server.setup.builtin;

import org.keycloak.admin.client.resource.RealmsResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.openremote.container.Container;
import org.openremote.manager.server.setup.AbstractKeycloakSetup;

import java.util.List;
import java.util.logging.Logger;

import static org.openremote.model.Constants.*;

public class KeycloakCleanSetup extends AbstractKeycloakSetup {

    private static final Logger LOG = Logger.getLogger(KeycloakCleanSetup.class.getName());

    public KeycloakCleanSetup(Container container) {
        super(container);
    }

    @Override
    public void onStart() throws Exception {
        super.onStart();

        // Delete all realms that are not the master realm
        LOG.info("Deleting all non-master realms");
        RealmsResource realmsResource = keycloakProvider.getRealms(accessToken);
        List<RealmRepresentation> realms = realmsResource.findAll();
        for (RealmRepresentation realmRepresentation : realms) {
            if (!realmRepresentation.getRealm().equals(MASTER_REALM)) {
                keycloakProvider.getRealms(accessToken).realm(realmRepresentation.getRealm()).remove();
            }
        }

        // Find out if there is a client already present for this application, if so, delete it
        masterClientsResource.findAll().stream()
            .filter(clientRepresentation -> clientRepresentation.getClientId().equals(KEYCLOAK_CLIENT_ID))
            .map(ClientRepresentation::getId)
            .forEach(clientObjectId -> {
                LOG.info("Deleting client: " + clientObjectId);
                masterClientsResource.get(clientObjectId).remove();
            });

        // Find out if there are any users except the admin, delete them
        masterUsersResource.search(null, null, null).stream()
            .filter(userRepresentation -> !userRepresentation.getUsername().equals(MASTER_REALM_ADMIN_USER))
            .map(userRepresentation -> {
                LOG.info("Deleting user: " + userRepresentation);
                return masterUsersResource.get(userRepresentation.getId());
            })
            .forEach(UserResource::remove);
    }
}
