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
import org.openremote.model.query.UserQuery;
import org.openremote.model.query.filter.TenantPredicate;

import java.util.Arrays;
import java.util.logging.Logger;

import static org.openremote.container.security.keycloak.KeycloakIdentityProvider.DEFAULT_CLIENTS;
import static org.openremote.model.Constants.MASTER_REALM;
import static org.openremote.model.Constants.MASTER_REALM_ADMIN_USER;

public class KeycloakCleanSetup extends AbstractKeycloakSetup {

    private static final Logger LOG = Logger.getLogger(KeycloakCleanSetup.class.getName());

    public KeycloakCleanSetup(Container container) {
        super(container);
    }

    @Override
    public void onStart() throws Exception {
        super.onStart();

        // Switch keycloak proxy back to admin cli
        keycloakProvider.setActiveCredentials(keycloakProvider.getDefaultKeycloakGrant(container));
        doClean();
    }

    protected void doClean() throws Exception {
        // Delete all realms that are not the master realm
        LOG.info("Deleting all non-master realms");
        Arrays.stream(keycloakProvider.getTenants()).forEach(tenant -> {
            if (!tenant.getRealm().equals(MASTER_REALM)) {
                LOG.info("Deleting tenant: " + tenant);
                keycloakProvider.deleteTenant(tenant.getRealm());
            }
        });

        LOG.info("Deleting all non-master admin users");
        Arrays.stream(keycloakProvider.queryUsers(new UserQuery().tenant(new TenantPredicate(MASTER_REALM)))).forEach(user -> {
            if (!user.getUsername().equals(MASTER_REALM_ADMIN_USER)) {
                LOG.info("Deleting user: " + user);
                keycloakProvider.deleteUser(MASTER_REALM, user.getId());
            }
        });

        // Delete all non built in clients
        LOG.info("Deleting all non default clients");
        Arrays.stream(keycloakProvider.getClients(MASTER_REALM)).forEach(client -> {
            if (!DEFAULT_CLIENTS.contains(client.getClientId())) {
                LOG.info("Deleting client: " + client.getClientId());
                keycloakProvider.deleteClient(MASTER_REALM, client.getClientId());
            }
        });
    }
}
