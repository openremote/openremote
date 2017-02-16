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
package org.openremote.manager.server.setup;

import org.keycloak.admin.client.resource.RealmsResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.openremote.container.Container;
import rx.Observable;

import java.util.List;

import static org.openremote.manager.shared.Constants.*;
import static rx.Observable.fromCallable;

public class KeycloakCleanSetup extends AbstractKeycloakSetup {

    public KeycloakCleanSetup(Container container) {
        super(container);
    }

    @Override
    public void execute() {

        // Delete all realms that are not the master realm
        RealmsResource realmsResource = identityService.getRealms(accessToken, false);
        List<RealmRepresentation> realms = realmsResource.findAll();
        for (RealmRepresentation realmRepresentation : realms) {
            if (!realmRepresentation.getRealm().equals(MASTER_REALM)) {
                identityService.getRealms(accessToken, false).realm(realmRepresentation.getRealm()).remove();
            }
        }

        // Find out if there is a client already present for this application, if so, delete it
        fromCallable(masterClientsResource::findAll)
            .flatMap(Observable::from)
            .filter(clientRepresentation -> clientRepresentation.getClientId().equals(KEYCLOAK_CLIENT_ID))
            .map(ClientRepresentation::getId)
            .subscribe(clientObjectId -> {
                masterClientsResource.get(clientObjectId).remove();
            });

        // Find out if there are any users except the admin, delete them
        fromCallable(() -> masterUsersResource.search(null, null, null))
            .flatMap(Observable::from)
            .filter(userRepresentation -> !userRepresentation.getUsername().equals(MASTER_REALM_ADMIN_USER))
            .map(userRepresentation -> masterUsersResource.get(userRepresentation.getId()))
            .subscribe(UserResource::remove);


    }
}
