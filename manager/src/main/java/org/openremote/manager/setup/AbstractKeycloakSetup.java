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

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;
import org.openremote.model.Container;
import org.openremote.model.security.ClientRole;
import org.openremote.model.security.Realm;
import org.openremote.model.security.User;
import org.openremote.model.setup.Setup;

import java.util.Arrays;
import java.util.Collections;

import static org.openremote.model.security.User.EMAIL_NOTIFICATIONS_DISABLED_ATTRIBUTE;
import static org.openremote.model.security.User.PUSH_NOTIFICATIONS_DISABLED_ATTRIBUTE;
import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID;

public abstract class AbstractKeycloakSetup implements Setup {

    public static final ClientRole[] PUBLIC_USER_ROLES = new ClientRole[] {
        ClientRole.READ_ASSETS,
        ClientRole.READ_MAP,
        ClientRole.READ_INSIGHTS
    };
    public static final ClientRole[] REGULAR_USER_ROLES = new ClientRole[] {
        ClientRole.WRITE_USER,
        ClientRole.READ_MAP,
        ClientRole.READ_ASSETS,
        ClientRole.READ_USERS,
        ClientRole.READ_RULES,
        ClientRole.READ_INSIGHTS,
        ClientRole.READ_ALARMS,
        ClientRole.WRITE_ASSETS,
        ClientRole.WRITE_ATTRIBUTES,
        ClientRole.WRITE_RULES,
        ClientRole.WRITE_INSIGHTS,
        ClientRole.WRITE_ALARMS
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

    protected Realm createRealm(String realmName, String displayName, boolean rememberMe) {
        Realm realm = new Realm();
        realm.setName(realmName);
        realm.setDisplayName(displayName);
        realm.setEnabled(true);
        realm.setDuplicateEmailsAllowed(true);
        realm.setRememberMe(rememberMe);
        realm = keycloakProvider.createRealm(realm);
        return realm;
    }

    protected User createUser(String realm, String username, String password, String firstName, String lastName, String email, boolean enabled, ClientRole[] roles) {
        return  createUser(realm, username, password, firstName, lastName, email, enabled, false, false, roles);
    }

    protected User createUser(String realm, String username, String password, String firstName, String lastName, String email, boolean enabled, boolean emailNotificationsDisabled, boolean pushNotificationsDisabled, ClientRole[] roles) {
        User user = new User();
        user.setUsername(username);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setEnabled(enabled);
        if (emailNotificationsDisabled) {
            user.setAttribute(EMAIL_NOTIFICATIONS_DISABLED_ATTRIBUTE, "true");
        }
        if (pushNotificationsDisabled) {
            user.setAttribute(PUSH_NOTIFICATIONS_DISABLED_ATTRIBUTE, "true");
        }
        user = keycloakProvider.createUpdateUser(realm, user, password, true);
        if (user == null) {
            return null;
        }
        if (roles != null && roles.length > 0) {
            keycloakProvider.updateUserRoles(realm, user.getId(), KEYCLOAK_CLIENT_ID, Arrays.stream(roles).map(ClientRole::getValue).toArray(String[]::new));
        }
        return user;
    }

    /**
     * Default realm roles will assign manage-account role to account client so we have to remove this role from the composite default roles
     * This is a temporary thing and when/if we move to groups we should look at explicit default roles on realm creation
     */
    protected void removeManageAccount(String realm) {
        keycloakProvider.<Void>getRealms(
            realmsResource -> {
                RealmResource realmResource = realmsResource.realm(realm);
                ClientRepresentation clientRepresentation = keycloakProvider.getClient(realm, "account");
                RoleResource roleResource = realmResource.roles().get("default-roles-" + realm);
                roleResource.getClientRoleComposites(clientRepresentation.getId())
                    .stream()
                    .filter(role -> role.getName().equals("manage-account"))
                    .findFirst()
                    .ifPresent(manageAccountRole -> roleResource.deleteComposites(Collections.singletonList(manageAccountRole)));
                return null;
            }
        );
    }
}
