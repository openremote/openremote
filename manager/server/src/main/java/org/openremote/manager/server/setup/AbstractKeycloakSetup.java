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

import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.openremote.container.Container;
import org.openremote.container.security.AuthForm;
import org.openremote.manager.server.security.ManagerIdentityService;
import rx.Observable;

import static org.openremote.manager.shared.Constants.KEYCLOAK_CLIENT_ID;
import static org.openremote.manager.shared.Constants.MASTER_REALM;
import static org.openremote.manager.shared.Constants.MASTER_REALM_ADMIN_USER;
import static rx.Observable.fromCallable;

public abstract class AbstractKeycloakSetup implements Setup {

    // We use this client ID to access Keycloak because by default it allows obtaining
    // an access token from authentication directly, which gives us full access to import/delete
    // demo data as needed.
    public static final String ADMIN_CLI_CLIENT_ID = "admin-cli";

    public static final String SETUP_KEYCLOAK_ADMIN_PASSWORD = "SETUP_KEYCLOAK_ADMIN_PASSWORD";
    public static final String SETUP_KEYCLOAK_ADMIN_PASSWORD_DEFAULT = "secret";

    final protected ManagerIdentityService identityService;
    final protected String accessToken;
    final protected RealmResource masterRealmResource;
    final protected ClientsResource masterClientsResource;
    final protected UsersResource masterUsersResource;

    public AbstractKeycloakSetup(Container container) {
        identityService = container.getService(ManagerIdentityService.class);

        // Use a non-proxy client to get the access token
        String demoAdminPassword = container.getConfig().getOrDefault(SETUP_KEYCLOAK_ADMIN_PASSWORD, SETUP_KEYCLOAK_ADMIN_PASSWORD_DEFAULT);
        this.accessToken = identityService.getKeycloak().getAccessToken(
            MASTER_REALM, new AuthForm(ADMIN_CLI_CLIENT_ID, MASTER_REALM_ADMIN_USER, demoAdminPassword)
        ).getToken();

        masterRealmResource = identityService.getRealms(accessToken, false).realm(MASTER_REALM);
        masterClientsResource = identityService.getRealms(accessToken, false).realm(MASTER_REALM).clients();
        masterUsersResource = identityService.getRealms(accessToken, false).realm(MASTER_REALM).users();
    }

    protected String getClientObjectId() {
        return fromCallable(() -> masterClientsResource.findByClientId(KEYCLOAK_CLIENT_ID))
            .flatMap(Observable::from)
            .map(ClientRepresentation::getId)
            .toBlocking().singleOrDefault(null);
    }

}
