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

import org.openremote.container.Container;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;
import org.openremote.model.security.ClientRole;
import org.openremote.model.security.Tenant;
import org.openremote.model.security.User;

public abstract class AbstractKeycloakSetup implements Setup {

    public static final String SETUP_EMAIL_FROM_KEYCLOAK = "SETUP_EMAIL_FROM_KEYCLOAK";
    public static final String SETUP_EMAIL_FROM_KEYCLOAK_DEFAULT = "no-reply@";
    public static final ClientRole[] PUBLIC_USER_ROLES = new ClientRole[] {
        ClientRole.READ_ASSETS,
        ClientRole.READ_MAP
    };
    public static final ClientRole[] REGULAR_USER_ROLES = new ClientRole[] {
        ClientRole.WRITE_USER,
        ClientRole.READ_MAP,
        ClientRole.READ_ASSETS,
        ClientRole.READ_RULES,
        ClientRole.WRITE_ASSETS,
        ClientRole.WRITE_RULES
    };

    final protected Container container;
    final protected ManagerIdentityService identityService;
    final protected ManagerKeycloakIdentityProvider keycloakProvider;
    final protected SetupService setupService;

    public AbstractKeycloakSetup(Container container) {
        this.container = container;
        this.identityService = container.getService(ManagerIdentityService.class);
        this.keycloakProvider = ((ManagerKeycloakIdentityProvider)identityService.getIdentityProvider());
        this.setupService = container.getService(SetupService.class);
    }

    public ManagerKeycloakIdentityProvider getKeycloakProvider() {
        return keycloakProvider;
    }

    protected Tenant createTenant(String realm, String displayName) {
        Tenant tenant = new Tenant();
        tenant.setRealm(realm);
        tenant.setDisplayName(displayName);
        tenant.setEnabled(true);
        tenant.setDuplicateEmailsAllowed(true);
        tenant = keycloakProvider.createTenant(tenant);
        return tenant;
    }

    protected Tenant createTenant(Tenant tenant) {
        return keycloakProvider.createTenant(tenant);
    }

    protected User createUser(String realm, String username, String password, String firstName, String lastName, String email, boolean enabled, ClientRole[] roles) {
        User user = new User();
        user.setUsername(username);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setEnabled(enabled);
        user = keycloakProvider.createUser(realm, user, password);
        if (user == null) {
            return null;
        }
        if (roles != null && roles.length > 0) {
            keycloakProvider.updateRoles(realm, user.getId(), roles);
        }
        return user;
    }
}
