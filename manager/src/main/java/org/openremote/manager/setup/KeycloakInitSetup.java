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

import org.openremote.container.util.UniqueIdentifierGenerator;
import org.openremote.model.Container;
import org.openremote.model.auth.OAuthClientCredentialsGrant;
import org.openremote.model.auth.OAuthGrant;
import org.openremote.model.security.ClientRole;
import org.openremote.model.security.Tenant;
import org.openremote.model.security.User;

import java.util.logging.Logger;

import static org.openremote.container.security.keycloak.KeycloakIdentityProvider.MANAGER_CLIENT_ID;
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
        Tenant masterRealm = keycloakProvider.getTenant(MASTER_REALM);
        masterRealm.setDisplayName("Master");
        keycloakProvider.updateTenant(masterRealm);

        // Create our client application with its default roles in the master realm
        keycloakProvider.createUpdateClient(masterRealm.getRealm(), keycloakProvider.generateOpenRemoteClientRepresentation());

        // Update master user name
        User adminUser = keycloakProvider.getUserByUsername(MASTER_REALM, MASTER_REALM_ADMIN_USER);
        adminUser.setFirstName("System");
        adminUser.setLastName("Administrator");
        keycloakProvider.createUpdateUser(MASTER_REALM, adminUser, null);

        // Give admin all roles on application client level
        keycloakProvider.updateUserRoles(MASTER_REALM, adminUser.getId(), KEYCLOAK_CLIENT_ID, ClientRole.READ.getValue(), ClientRole.WRITE.getValue());

        // Create a service client for this manager to communicate with keycloak so it doesn't use the admin user credentials
        User keycloakProxyUser = new User().setUsername(MANAGER_CLIENT_ID).setServiceAccount(true).setSystemAccount(true);
        String secret = UniqueIdentifierGenerator.generateId();

        // Use credentials from file system if they are available
        OAuthGrant storedGrant = keycloakProvider.loadCredentials();
        if (storedGrant != null) {
            keycloakProxyUser.setUsername(storedGrant.getClientId());
            secret = storedGrant.getClientSecret() != null ? storedGrant.getClientSecret() : secret;
        }

        keycloakProxyUser = keycloakProvider.createUpdateUser(MASTER_REALM, keycloakProxyUser, secret);

        // Give this user admin realm role (so they can do realm CRUD)
        keycloakProvider.updateUserRoles(MASTER_REALM, keycloakProxyUser.getId(), null, "admin");

        OAuthClientCredentialsGrant grant = new OAuthClientCredentialsGrant(keycloakProvider.getTokenUri(MASTER_REALM).toString(), keycloakProxyUser.getUsername(), secret, null);
        keycloakProvider.setActiveCredentials(grant);
        keycloakProvider.saveCredentials(grant);
    }
}
