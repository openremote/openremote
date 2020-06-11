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

import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.openremote.container.Container;
import org.openremote.container.security.PasswordAuthForm;
import org.openremote.container.security.keycloak.KeycloakIdentityProvider;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;

import static org.openremote.manager.security.ManagerIdentityProvider.SETUP_ADMIN_PASSWORD;
import static org.openremote.manager.security.ManagerIdentityProvider.SETUP_ADMIN_PASSWORD_DEFAULT;
import static org.openremote.model.Constants.MASTER_REALM;
import static org.openremote.model.Constants.MASTER_REALM_ADMIN_USER;

public abstract class AbstractKeycloakSetup implements Setup {

    public static final String SETUP_EMAIL_FROM_KEYCLOAK = "SETUP_EMAIL_FROM_KEYCLOAK";
    public static final String SETUP_EMAIL_FROM_KEYCLOAK_DEFAULT = "no-reply@";

    final protected Container container;
    final protected ManagerIdentityService identityService;
    final protected ManagerKeycloakIdentityProvider keycloakProvider;
    final protected SetupService setupService;
    protected String accessToken;
    protected RealmResource masterRealmResource;
    protected ClientsResource masterClientsResource;
    protected UsersResource masterUsersResource;

    public AbstractKeycloakSetup(Container container) {
        this.container = container;
        this.identityService = container.getService(ManagerIdentityService.class);
        this.keycloakProvider = ((ManagerKeycloakIdentityProvider)identityService.getIdentityProvider());
        this.setupService = container.getService(SetupService.class);
    }

    public ManagerKeycloakIdentityProvider getKeycloakProvider() {
        return keycloakProvider;
    }

    @Override
    public void onStart() throws Exception {
        // Use direct access grant feature of Keycloak Admin CLI to get superuser access token
        String keycloakAdminPassword = container.getConfig().getOrDefault(SETUP_ADMIN_PASSWORD, SETUP_ADMIN_PASSWORD_DEFAULT);
        this.accessToken = keycloakProvider.getKeycloak().getAccessToken(
            MASTER_REALM, new PasswordAuthForm(KeycloakIdentityProvider.ADMIN_CLI_CLIENT_ID, MASTER_REALM_ADMIN_USER, keycloakAdminPassword)
        ).getToken();

        masterRealmResource = keycloakProvider.getRealms(accessToken).realm(MASTER_REALM);
        masterClientsResource = masterRealmResource.clients();
        masterUsersResource = masterRealmResource.users();
    }

    protected String getClientObjectId(ClientsResource clientsResource, String clientName) {
        return clientsResource.findByClientId(clientName)
            .stream()
            .map(ClientRepresentation::getId)
            .findFirst().orElseThrow(() -> new RuntimeException("Client object ID not found for client name: " + clientName));
    }
}
