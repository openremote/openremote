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

import elemental.client.Browser;
import elemental.html.Location;
import org.openremote.manager.client.event.UserChangeEvent;
import org.openremote.manager.client.interop.Consumer;
import org.openremote.manager.client.interop.keycloak.AuthToken;
import org.openremote.manager.client.interop.keycloak.Keycloak;
import org.openremote.manager.client.interop.keycloak.KeycloakCallback;
import org.openremote.manager.client.interop.keycloak.LogoutOptions;
import org.openremote.manager.shared.http.RequestParams;
import org.openremote.model.Constants;
import org.openremote.model.event.bus.EventBus;

import java.util.logging.Logger;

public class KeycloakSecurityService implements SecurityService {

    private static final Logger LOG = Logger.getLogger(KeycloakSecurityService.class.getName());

    protected final Keycloak keycloak;

    public KeycloakSecurityService(Keycloak keycloak,
                                   CookieService cookieService,
                                   EventBus eventBus) {
        this.keycloak = keycloak;

        onAuthSuccess(() -> eventBus.dispatch(
            new UserChangeEvent(getParsedToken().getPreferredUsername())
        ));

        onAuthLogout(() -> {
            eventBus.dispatch(new UserChangeEvent(null));
        });

        // We update the refresh token in the background. The only other option would
        // be to update the token before every request. However, this
        // will force asynchronous execution of all HTTP requests, which we don't want.
        Browser.getWindow().setInterval(() -> updateToken(
            Constants.ACCESS_TOKEN_LIFESPAN_SECONDS/2, // Token must be good for X more seconds
            tokenRefreshed -> LOG.fine("Access token updated, was refreshed from auth server: " + tokenRefreshed),
            this::logout
        ), 30000);
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
        LogoutOptions opts = new LogoutOptions();
        // TODO Experiments with relative URLs only worked for master realm logout, not any other realm...
        Location location = Browser.getWindow().getLocation();
        opts.redirectUri = location.getProtocol() + "//" + location.getHost() + "/" + getAuthenticatedRealm();
        keycloak.logout(opts);
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
        // TODO If the periodic refresh of token (see above) is not enough, this is where we could refresh
        requestParams.withBearerAuth(getToken());
    }

    @Override
    public String setCredentials(String serviceUrl) {
        // TODO If the periodic refresh of token (see above) is not enough, this is where we could refresh
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

    protected void updateToken(int minValiditySeconds, Consumer<Boolean> successFn, Runnable errorFn) {
        KeycloakCallback kcCallback = keycloak.updateToken(minValiditySeconds);
        kcCallback.success(new org.openremote.manager.client.interop.Consumer<Boolean>(){
            @Override
            public void accept(Boolean o) {
                successFn.accept(o);
            }
        });
        kcCallback.error(new org.openremote.manager.client.interop.Runnable(){
            @Override
            public void run() {
                errorFn.run();
            }
        });
    }
}
