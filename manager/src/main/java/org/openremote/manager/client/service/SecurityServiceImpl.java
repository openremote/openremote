package org.openremote.manager.client.service;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import org.openremote.manager.client.interop.keycloak.*;
import org.openremote.manager.shared.Consumer;
import org.openremote.manager.shared.Runnable;

public class SecurityServiceImpl implements SecurityService {

    private Keycloak keycloak;
    private CookieService cookieService;
    private EventBus eventBus;

    @Inject
    public SecurityServiceImpl(Keycloak keycloak,
                               CookieService cookieService,
                               EventBus eventBus) {
        this.keycloak = keycloak;
        this.cookieService = cookieService;
        this.eventBus = eventBus;

        // TODO Add event handlers for security service?
        // onAuthLogout(() -> {
        //  eventBus.fireEvent(new UserChangeEvent(null));
        // });
        //
        // onAuthSuccess(() -> {
        //  eventBus.fireEvent(new UserChangeEvent(securityService.getUsername()));
        // });
    }

    @Override
    public String getUsername() {
        String name = null;
        return name;
    }

    @Override
    public String getXsrfToken() {
        return cookieService.getCookie("XSRF-TOKEN");
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
        keycloak.logout();
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
        keycloak.onAuthSuccess(fn);
    }

    @Override
    public void onAuthLogout(Runnable fn) {
        keycloak.onAuthLogout(fn);
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
    public String getRefreshToken() {
        return keycloak.refreshToken;
    }
}
