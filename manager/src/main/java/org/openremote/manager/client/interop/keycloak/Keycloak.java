package org.openremote.manager.client.interop.keycloak;

import elemental.json.JsonArray;
import elemental.json.JsonObject;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(isNative = true, namespace = "")
public class Keycloak {
    @JsProperty
    public boolean authenticated;

    @JsProperty
    public String token;

    @JsProperty
    public String realm;

    @JsProperty
    public String clientId;

    @JsProperty
    public JsonObject tokenParsed;

    @JsProperty
    public String subject;

    @JsProperty
    public String idToken;

    @JsProperty
    public JsonObject idTokenParsed;

    @JsProperty
    public JsonArray realmAceess;

    @JsProperty
    public JsonArray resourceAccess;

    @JsProperty
    public String refreshToken;

    @JsProperty
    public JsonObject refreshTokenParsed;

    @JsProperty
    public int timeSkew;

    @JsProperty
    public String responseMode;

    @JsProperty
    public String flow;

    @JsProperty
    public String responseType;

    public Keycloak(String configUrl) {
    }

    public Keycloak(KeycloakConfig config) {
    }

    public native KeycloakCallback init(InitOptions options);

    public native KeycloakCallback init();

    public native void login();

    public native void login(LoginOptions options);

    public native void logout(LogoutOptions options);

    public native boolean hasRealmRole(String role);

    public native boolean hasResourceRole(String role, String resource);

    public native boolean isTokenExpired();

    public native boolean isTokenExpired(int minValiditySeconds);

    public native KeycloakCallback updateToken();

    public native KeycloakCallback updateToken(int minValiditySeconds);

    public native void clearToken();

    public native void onTokenExpired(ExpiredFn expiredFn);

    public native void onAuthSuccess(ExpiredFn expiredFn);
}
