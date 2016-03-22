package org.openremote.manager.client.service;

import elemental.json.JsonObject;
import org.openremote.manager.client.interop.keycloak.LoginOptions;
import org.openremote.manager.client.interop.keycloak.LogoutOptions;
import org.openremote.manager.shared.Consumer;
import org.openremote.manager.shared.Runnable;

public interface SecurityService {

    public static final int MIN_VALIDITY_SECONDS = 10;

    String getUsername();

    void login();

    void login(LoginOptions options);

    void logout();

    void logout(LogoutOptions options);

    void register();

    void register(LoginOptions options);

    boolean hasRealmRole(String role);

    boolean hasResourceRole(String role, String resource);

    boolean isTokenExpired();

    boolean isTokenExpired(int minValiditySeconds);

    void clearToken();

    void onTokenExpired(Runnable expiredFn);

    void onAuthSuccess(Runnable expiredFn);

    void onAuthLogout(Runnable expiredFn);

    void updateToken(Consumer<Boolean> successFn, Runnable errorFn);

    void updateToken(int minValiditySeconds, Consumer<Boolean> successFn, Runnable errorFn);

    String getRealm();

    String getToken();

    JsonObject getParsedToken();

    String getRefreshToken();
}
