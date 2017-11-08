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
import org.openremote.manager.server.security.ManagerKeycloakIdentityProvider;
import org.openremote.manager.shared.security.TenantEmailConfig;

import static org.openremote.container.util.MapAccess.getBoolean;
import static org.openremote.container.util.MapAccess.getInteger;
import static org.openremote.model.Constants.*;

public abstract class AbstractKeycloakSetup implements Setup {

    // We use this client ID to access Keycloak because by default it allows obtaining
    // an access token from authentication directly, which gives us full access to import/delete
    // demo data as needed.
    public static final String ADMIN_CLI_CLIENT_ID = "admin-cli";

    public static final String KEYCLOAK_PASSWORD = "KEYCLOAK_PASSWORD";
    public static final String KEYCLOAK_PASSWORD_DEFAULT = "secret";
    public static final String SETUP_KEYCLOAK_EMAIL_HOST = "SETUP_KEYCLOAK_EMAIL_HOST";
    public static final String SETUP_KEYCLOAK_EMAIL_HOST_DEFAULT = "smtp-host.demo.tld";
    public static final String SETUP_KEYCLOAK_EMAIL_USER = "SETUP_KEYCLOAK_EMAIL_USER";
    public static final String SETUP_KEYCLOAK_EMAIL_USER_DEFAULT = "smtp-user";
    public static final String SETUP_KEYCLOAK_EMAIL_PASSWORD = "SETUP_KEYCLOAK_EMAIL_PASSWORD";
    public static final String SETUP_KEYCLOAK_EMAIL_PASSWORD_DEFAULT = "smtp-password";
    public static final String SETUP_KEYCLOAK_EMAIL_PORT = "SETUP_KEYCLOAK_EMAIL_PORT";
    public static final int SETUP_KEYCLOAK_EMAIL_PORT_DEFAULT = 25;
    public static final String SETUP_KEYCLOAK_EMAIL_AUTH = "SETUP_KEYCLOAK_EMAIL_AUTH";
    public static final boolean SETUP_KEYCLOAK_EMAIL_AUTH_DEFAULT = true;
    public static final String SETUP_KEYCLOAK_EMAIL_TLS = "SETUP_KEYCLOAK_EMAIL_TLS";
    public static final boolean SETUP_KEYCLOAK_EMAIL_TLS_DEFAULT = true;
    public static final String SETUP_KEYCLOAK_EMAIL_FROM = "SETUP_KEYCLOAK_EMAIL_FROM";
    public static final String SETUP_KEYCLOAK_EMAIL_FROM_DEFAULT = "noreply@demo.tld";

    final protected Container container;
    final protected ManagerIdentityService identityService;
    final protected ManagerKeycloakIdentityProvider keycloakProvider;
    final protected SetupService setupService;
    final protected TenantEmailConfig emailConfig;
    protected String accessToken;
    protected RealmResource masterRealmResource;
    protected ClientsResource masterClientsResource;
    protected UsersResource masterUsersResource;

    public AbstractKeycloakSetup(Container container) {
        this.container = container;
        this.identityService = container.getService(ManagerIdentityService.class);
        this.keycloakProvider = ((ManagerKeycloakIdentityProvider)identityService.getIdentityProvider());
        this.setupService = container.getService(SetupService.class);

        // Configure SMTP
        if (container.getConfig().containsKey(SETUP_KEYCLOAK_EMAIL_HOST)) {
            emailConfig = new TenantEmailConfig();
            emailConfig.setHost(container.getConfig().getOrDefault(SETUP_KEYCLOAK_EMAIL_HOST, SETUP_KEYCLOAK_EMAIL_HOST_DEFAULT));
            emailConfig.setStarttls(getBoolean(container.getConfig(), SETUP_KEYCLOAK_EMAIL_TLS,SETUP_KEYCLOAK_EMAIL_TLS_DEFAULT));
            emailConfig.setPort(getInteger(container.getConfig(), SETUP_KEYCLOAK_EMAIL_PORT, SETUP_KEYCLOAK_EMAIL_PORT_DEFAULT));
            emailConfig.setAuth(getBoolean(container.getConfig(), SETUP_KEYCLOAK_EMAIL_AUTH, SETUP_KEYCLOAK_EMAIL_AUTH_DEFAULT));
            emailConfig.setUser(container.getConfig().getOrDefault(SETUP_KEYCLOAK_EMAIL_USER, SETUP_KEYCLOAK_EMAIL_USER_DEFAULT));
            emailConfig.setPassword(container.getConfig().getOrDefault(SETUP_KEYCLOAK_EMAIL_PASSWORD, SETUP_KEYCLOAK_EMAIL_PASSWORD_DEFAULT));
            emailConfig.setFrom(container.getConfig().getOrDefault(SETUP_KEYCLOAK_EMAIL_FROM, SETUP_KEYCLOAK_EMAIL_FROM_DEFAULT));
        } else {
            emailConfig = null;
        }
    }

    public ManagerKeycloakIdentityProvider getKeycloakProvider() {
        return keycloakProvider;
    }

    @Override
    public void onStart() throws Exception {
        // Use direct access grant feature of Keycloak Admin CLI to get superuser access token
        String keycloakAdminPassword = container.getConfig().getOrDefault(KEYCLOAK_PASSWORD, KEYCLOAK_PASSWORD_DEFAULT);
        this.accessToken = keycloakProvider.getKeycloak().getAccessToken(
            MASTER_REALM, new AuthForm(ADMIN_CLI_CLIENT_ID, MASTER_REALM_ADMIN_USER, keycloakAdminPassword)
        ).getToken();

        masterRealmResource = keycloakProvider.getRealms(accessToken).realm(MASTER_REALM);
        masterClientsResource = keycloakProvider.getRealms(accessToken).realm(MASTER_REALM).clients();
        masterUsersResource = keycloakProvider.getRealms(accessToken).realm(MASTER_REALM).users();
    }

    protected String getClientObjectId(ClientsResource clientsResource) {
        return clientsResource.findByClientId(KEYCLOAK_CLIENT_ID)
            .stream()
            .map(ClientRepresentation::getId)
            .findFirst().orElseThrow(() -> new RuntimeException("Client object ID not found: " + KEYCLOAK_CLIENT_ID));
    }

}
