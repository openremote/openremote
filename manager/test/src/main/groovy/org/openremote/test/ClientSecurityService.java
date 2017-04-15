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
import org.openremote.manager.client.interop.keycloak.LoginOptions;
import org.openremote.manager.client.interop.keycloak.LogoutOptions;
import org.openremote.manager.client.service.SecurityService;
import org.openremote.model.Constants;

import java.util.function.Consumer;

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
    public void login() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void login(LoginOptions options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void logout() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void logout(LogoutOptions options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void register() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void register(LoginOptions options) {
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
    public boolean isTokenExpired() {
        updateAccessToken();
        return false;
    }

    @Override
    public boolean isTokenExpired(int minValiditySeconds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clearToken() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onTokenExpired(Runnable expiredFn) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onAuthSuccess(Runnable expiredFn) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onAuthLogout(Runnable expiredFn) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateToken(Consumer<Boolean> successFn, Runnable errorFn) {
        successFn.accept(true);
    }

    @Override
    public void updateToken(int minValiditySeconds, Consumer<Boolean> successFn, Runnable errorFn) {
        successFn.accept(true);
    }

    @Override
    public String getAuthenticatedRealm() {
        return keycloakDeployment.getRealm();
    }

    @Override
    public String getToken() {
        updateAccessToken();
        return token;
    }

    @Override
    public AuthToken getParsedToken() {
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

    @Override
    public String getRefreshToken() {
        throw new UnsupportedOperationException();
    }
}
