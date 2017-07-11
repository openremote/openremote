/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.test;

import groovy.lang.Closure;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.rotation.AdapterRSATokenVerifier;
import org.keycloak.representations.AccessToken;
import org.openremote.manager.client.interop.keycloak.AuthToken;
import org.openremote.manager.client.service.SecurityService;
import org.openremote.manager.shared.http.RequestParams;
import org.openremote.model.Constants;

/**
 * Does the same job as keycloak.js implementation.
 */
public class ClientSecurityService implements SecurityService {

    final protected KeycloakDeployment keycloakDeployment;
    final protected Closure<String> accessTokenClosure;

    protected String token;
    protected AccessToken accessToken;

    public ClientSecurityService(KeycloakDeployment keycloakDeployment, Closure<String> accessTokenClosure) {
        this.keycloakDeployment = keycloakDeployment;
        this.accessTokenClosure = accessTokenClosure;
    }

    protected void updateAccessToken() {
        token = accessTokenClosure.call();
        try {
            accessToken = AdapterRSATokenVerifier.verifyToken(token, keycloakDeployment);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
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
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSuperUser() {
        return keycloakDeployment.getRealm().equals(Constants.MASTER_REALM) && hasRealmRole(Constants.REALM_ADMIN_ROLE);
    }

    @Override
    public boolean hasRealmRole(String role) {
        updateAccessToken();
        return accessToken.getRealmAccess().isUserInRole(role);
    }

    @Override
    public boolean hasResourceRole(String role, String resource) {
        updateAccessToken();
        return accessToken.getResourceAccess().containsKey(resource)
            && accessToken.getResourceAccess().get(resource).isUserInRole(role);
    }

    @Override
    public boolean hasResourceRoleOrIsSuperUser(String role, String resource) {
        return hasResourceRole(role, resource) || isSuperUser();
    }

    @Override
    public String getAuthenticatedRealm() {
        return keycloakDeployment.getRealm();
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

    protected String getToken() {
        updateAccessToken();
        return token;
    }

    protected AuthToken getParsedToken() {
        updateAccessToken();
        return new AuthToken() {
            @Override
            public String getName() {
                return accessToken.getName();
            }

            @Override
            public String getPreferredUsername() {
                return accessToken.getPreferredUsername();
            }
        };
    }

}
