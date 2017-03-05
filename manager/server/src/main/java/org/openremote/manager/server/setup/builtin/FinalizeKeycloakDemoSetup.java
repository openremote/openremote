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
package org.openremote.manager.server.setup.builtin;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.openremote.container.Container;
import org.openremote.manager.server.setup.AbstractKeycloakSetup;
import org.openremote.model.asset.ProtectedUserAssets;

/**
 * Things that must be done after Keycloak and Manager data exists, e.g. linking both.
 */
public class FinalizeKeycloakDemoSetup extends AbstractKeycloakSetup {

    public FinalizeKeycloakDemoSetup(Container container) {
        super(container);
    }

    @Override
    public void execute() throws Exception {

        // Restrict access to assets
        UsersResource customerAUsersResource = identityService.getRealms(accessToken, false).realm("customerA").users();
        UserRepresentation testuser3 = customerAUsersResource.search("testuser3", null, null, null, null, null).get(0);
        UserResource testuser3Resource = customerAUsersResource.get(testuser3.getId());
        ManagerDemoSetup managerDemoSetup = setupService.getTaskOfType(ManagerDemoSetup.class);
        testuser3.setAttributes(ProtectedUserAssets.createUserAttributes(
            managerDemoSetup.apartment1Id,
            managerDemoSetup.apartment1LivingroomId,
            managerDemoSetup.apartment1LivingroomThermostatId,
            managerDemoSetup.apartment2Id
        ));
        testuser3Resource.update(testuser3);
    }
}
