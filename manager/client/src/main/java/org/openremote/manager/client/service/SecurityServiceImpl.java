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

import com.google.inject.Inject;
import elemental.client.Browser;
import elemental.html.Location;
import org.openremote.manager.client.event.UserChangeEvent;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.interop.keycloak.*;
import org.openremote.manager.shared.Constants;
import org.openremote.manager.shared.Consumer;
import org.openremote.manager.shared.Runnable;

import java.util.logging.Logger;

public class SecurityServiceImpl implements SecurityService {

    private static final Logger LOG = Logger.getLogger(SecurityServiceImpl.class.getName());

    protected final Keycloak keycloak;

    @Inject
    public SecurityServiceImpl(Keycloak keycloak,
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
        // be to update the token before every request in request service. However, this
        // will force asynchronous execution of all HTTP requests, which we don't want.
        Browser.getWindow().setInterval(() -> updateToken(
            Constants.ACCESS_TOKEN_LIFESPAN_SECONDS/2, // Token must be good for X more seconds
            tokenRefreshed -> LOG.fine("Access token updated, was refreshed from auth server: " + tokenRefreshed),
            this::logout
        ), 30000);
    }

    @Override
    public void login() {
        keycloak.login();
    }

    @Override
    public void login(LoginOptions options) {
        keycloak.login(options);
    }

    @Override
    public void logout() {
        LogoutOptions opts = new LogoutOptions();
        // TODO Experiments with relative URLs only worked for master realm logout, not any other realm...
        Location location = Browser.getWindow().getLocation();
        opts.redirectUri = location.getProtocol() + "//" + location.getHost() + "/" + getRealm();
        keycloak.logout(opts);
    }

    @Override
    public void logout(LogoutOptions options) {
        keycloak.logout(options);
    }

    @Override
    public void register() {
        keycloak.register();
    }

    @Override
    public void register(LoginOptions options) {
        keycloak.register(options);
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
    public boolean hasResourceRoleOrIsAdmin(String role, String resource) {
        return keycloak.hasResourceRole(role, resource) || keycloak.hasRealmRole("admin");
    }

    @Override
    public boolean isTokenExpired() {
        return keycloak.isTokenExpired();
    }

    @Override
    public boolean isTokenExpired(int minValiditySeconds) {
        return keycloak.isTokenExpired(minValiditySeconds);
    }

    @Override
    public void clearToken() {
        keycloak.clearToken();
    }

    @Override
    public void onTokenExpired(Runnable fn) {
        keycloak.onTokenExpired(fn);
    }

    @Override
    public void onAuthSuccess(Runnable fn) {
        keycloak.onAuthSuccess = fn;
    }

    @Override
    public void onAuthLogout(Runnable fn) {
        keycloak.onAuthLogout = fn;
    }

    @Override
    public void updateToken(Consumer<Boolean> successFn, Runnable errorFn) {
        KeycloakCallback kcCallback = keycloak.updateToken();
        kcCallback.success(successFn);
        kcCallback.error(errorFn);
    }

    @Override
    public void updateToken(int minValiditySeconds, Consumer<Boolean> successFn, Runnable errorFn) {
        KeycloakCallback kcCallback = keycloak.updateToken(minValiditySeconds);
        kcCallback.success(successFn);
        kcCallback.error(errorFn);
    }

    @Override
    public String getRealm() {
        return keycloak.realm;
    }

    @Override
    public String getToken() {
        return keycloak.token;
    }

    @Override
    public AuthToken getParsedToken() {
        return keycloak.tokenParsed;
    }

    @Override
    public String getRefreshToken() {
        return keycloak.refreshToken;
    }
}
