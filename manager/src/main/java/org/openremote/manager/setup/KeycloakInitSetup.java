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
package org.openremote.manager.setup;

import org.openremote.model.Container;
import org.openremote.model.security.ClientRole;
import org.openremote.model.security.Realm;
import org.openremote.model.security.User;

import java.util.logging.Logger;

import static org.openremote.model.Constants.*;

public class KeycloakInitSetup extends AbstractKeycloakSetup {

    private static final Logger LOG = Logger.getLogger(KeycloakInitSetup.class.getName());

    public KeycloakInitSetup(Container container) {
        super(container);
    }

    @Override
    public void onStart() throws Exception {
        super.onStart();

        // Update the master realm which is auto created by Keycloak itself
        // This will cause the keycloak provider to configure it appropriately
        // e.g. Set SMTP server, theme, timeouts, etc.
        Realm masterRealm = keycloakProvider.getRealm(MASTER_REALM);
        masterRealm.setDisplayName("Master");
        masterRealm.setRealmRoles(masterRealm.getNormalisedRealmRoles());
        keycloakProvider.updateRealm(masterRealm);

        // Create our client application with its default roles in the master realm
        keycloakProvider.createUpdateClient(masterRealm.getName(), keycloakProvider.generateOpenRemoteClientRepresentation());

        // Update master user name
        User adminUser = keycloakProvider.getUserByUsername(MASTER_REALM, MASTER_REALM_ADMIN_USER);
        adminUser.setFirstName("System");
        adminUser.setLastName("Administrator");
        keycloakProvider.createUpdateUser(MASTER_REALM, adminUser, null, true);

        // Give admin all roles on application client level
        keycloakProvider.updateUserRoles(MASTER_REALM, adminUser.getId(), KEYCLOAK_CLIENT_ID, ClientRole.READ.getValue(), ClientRole.WRITE.getValue());
    }
}
