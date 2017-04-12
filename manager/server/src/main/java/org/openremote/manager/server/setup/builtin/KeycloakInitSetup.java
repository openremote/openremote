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

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.openremote.container.Container;
import org.openremote.manager.server.setup.AbstractKeycloakSetup;
import org.openremote.manager.shared.security.ClientRole;
import rx.Observable;

import java.util.Arrays;
import java.util.logging.Logger;

import static org.openremote.model.Constants.*;
import static rx.Observable.fromCallable;

public class KeycloakInitSetup extends AbstractKeycloakSetup {

    private static final Logger LOG = Logger.getLogger(KeycloakInitSetup.class.getName());

    public KeycloakInitSetup(Container container) {
        super(container);
    }

    @Override
    public void execute() {
        // Configure the master realm
        RealmRepresentation masterRealm = masterRealmResource.toRepresentation();

        masterRealm.setDisplayName("Master");

        // Set theme, timeouts, etc.
        identityService.configureRealm(masterRealm);

        masterRealmResource.update(masterRealm);

        // Create our client application with its default roles
        identityService.createClientApplication(null, accessToken, masterRealm.getRealm());

        // Get the client application ID so we can assign roles to users at the client
        // level (we can only check realm _or_ client application roles in @RolesAllowed!)
        String clientObjectId = getClientObjectId(masterClientsResource);

        ClientResource clientResource = masterClientsResource.get(clientObjectId);
        RolesResource rolesResource = clientResource.roles();

        // Give admin all roles on application client level
        RoleRepresentation readRole = rolesResource.get(ClientRole.READ.getValue()).toRepresentation();
        RoleRepresentation writeRole = rolesResource.get(ClientRole.WRITE.getValue()).toRepresentation();

        fromCallable(() -> masterUsersResource.search(MASTER_REALM_ADMIN_USER, null, null, null, null, null))
            .flatMap(Observable::from)
            .map(userRepresentation -> masterUsersResource.get(userRepresentation.getId()))
            .subscribe(adminUser -> {
                    adminUser.roles().clientLevel(clientObjectId).add(Arrays.asList(
                        readRole,
                        writeRole
                    ));
                    LOG.info("Assigned all application roles to 'admin' user");
                    UserRepresentation adminRep = adminUser.toRepresentation();
                    adminRep.setFirstName("System");
                    adminRep.setLastName("Administrator");
                    adminUser.update(adminRep);
                }
            );
    }
}
