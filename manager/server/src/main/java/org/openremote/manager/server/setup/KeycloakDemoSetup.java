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

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.openremote.container.Container;
import org.openremote.manager.shared.security.ClientRole;
import org.openremote.manager.shared.security.Tenant;

import java.util.Arrays;
import java.util.logging.Logger;

public class KeycloakDemoSetup extends AbstractKeycloakSetup {

    private static final Logger LOG = Logger.getLogger(KeycloakDemoSetup.class.getName());

    public KeycloakDemoSetup(Container container) {
        super(container);
    }

    @Override
    public void execute() throws Exception {
        String clientObjectId = getClientObjectId();
        ClientResource clientResource = masterClientsResource.get(clientObjectId);
        RolesResource rolesResource = clientResource.roles();

        // Create a new 'test' user in master realm
        UserRepresentation testUser = new UserRepresentation();
        testUser.setUsername("test");
        testUser.setFirstName("Testuserfirst");
        testUser.setLastName("Testuserlast");
        testUser.setEnabled(true);
        masterUsersResource.create(testUser);
        testUser = masterUsersResource.search("test", null, null, null, null, null).get(0);

        CredentialRepresentation testUserCredential = new CredentialRepresentation();
        testUserCredential.setType("password");
        testUserCredential.setValue("test");
        testUserCredential.setTemporary(false);
        masterUsersResource.get(testUser.getId()).resetPassword(testUserCredential);

        // Assign roles to the 'test' user
        RoleRepresentation readRole = rolesResource.get(ClientRole.READ.getValue()).toRepresentation();
        RoleRepresentation writeRole = rolesResource.get(ClientRole.WRITE.getValue()).toRepresentation();
        masterUsersResource.get(testUser.getId()).roles().clientLevel(clientObjectId).add(Arrays.asList(
            readRole,
            writeRole
        ));

        LOG.info("Added master realm user '" + testUser.getUsername() + "' with password '" + testUserCredential.getValue() + "'");

        // Create additional test realms/tenants
        Tenant customerA = new Tenant();
        customerA.setRealm("customerA");
        customerA.setDisplayName("Customer A");
        customerA.setEnabled(true);
        identityService.createTenant(accessToken, customerA);

        Tenant customerB = new Tenant();
        customerB.setRealm("customerB");
        customerB.setDisplayName("Customer B");
        customerB.setEnabled(true);
        identityService.createTenant(accessToken, customerB);
    }
}
