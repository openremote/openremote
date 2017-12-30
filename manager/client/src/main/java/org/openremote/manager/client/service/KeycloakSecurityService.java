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
package org.openremote.manager.client.service;

import org.openremote.manager.client.app.OpenRemoteApp;
import org.openremote.manager.client.interop.keycloak.AuthToken;
import org.openremote.manager.client.interop.keycloak.Keycloak;
import org.openremote.manager.shared.http.RequestParams;
import org.openremote.model.Constants;
import org.openremote.model.event.bus.EventBus;

import java.util.logging.Logger;

public class KeycloakSecurityService implements SecurityService {

    private static final Logger LOG = Logger.getLogger(KeycloakSecurityService.class.getName());

    protected final OpenRemoteApp openRemoteApp;
    protected final Keycloak keycloak;

    public KeycloakSecurityService(OpenRemoteApp openRemoteApp,
                                   CookieService cookieService,
                                   EventBus eventBus) {
        this.openRemoteApp = openRemoteApp;
        this.keycloak = openRemoteApp.getKeycloak();
    }

    @Override
    public String getUsername() {
        return getParsedToken().getPreferredUsername();
    }

    @Override
    public String getFullName() {
        return getParsedToken().getName();
    }

    @Override
    public void logout() {
        openRemoteApp.logout();
    }

    @Override
    public boolean isSuperUser() {
        return getAuthenticatedRealm().equals(Constants.MASTER_REALM) && hasRealmRole(Constants.REALM_ADMIN_ROLE);
    }

    @Override
    public boolean hasRealmRole(String role) {
        return keycloak.hasRealmRole(role);
    }

    @Override
    public boolean hasResourceRole(String role, String resource) {
        return keycloak.hasResourceRole(role, resource);
    }

    @Override
    public boolean hasResourceRoleOrIsSuperUser(String role, String resource) {
        return hasResourceRole(role, resource) || isSuperUser();
    }

    @Override
    public String getAuthenticatedRealm() {
        return keycloak.realm;
    }

    @Override
    public <OUT> void setCredentials(RequestParams<OUT> requestParams) {
        requestParams.withBearerAuth(getToken());
    }

    @Override
    public String setCredentials(String serviceUrl) {
        String authenticatedServiceUrl = serviceUrl
            + "?Auth-Realm=" + getAuthenticatedRealm()
            + "&Authorization=Bearer " + getToken();
        return authenticatedServiceUrl;
    }

    @Override
    public boolean isUserTenantAdminEnabled() {
        return true;
    }

    protected String getToken() {
        return keycloak.token;
    }

    protected AuthToken getParsedToken() {
        return keycloak.tokenParsed;
    }

    protected void onAuthSuccess(Runnable fn) {
        keycloak.onAuthSuccess = () -> fn.run();
    }

    protected void onAuthLogout(Runnable fn) {
        keycloak.onAuthLogout = () -> fn.run();
    }

}
