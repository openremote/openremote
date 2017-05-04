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
package org.openremote.manager.client.interop.keycloak;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import org.openremote.manager.client.interop.Runnable;

import static jsinterop.annotations.JsPackage.GLOBAL;

@JsType(isNative = true, namespace = GLOBAL)
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
    public AuthToken tokenParsed;

    @JsProperty
    public String subject;

    @JsProperty
    public String idToken;

    @JsProperty
    public JavaScriptObject idTokenParsed;

    @JsProperty
    public JavaScriptObject realmAccess;

    @JsProperty
    public JavaScriptObject resourceAccess;

    @JsProperty
    public String refreshToken;

    @JsProperty
    public JavaScriptObject refreshTokenParsed;

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

    public native void logout();

    public native void logout(LogoutOptions options);

    public native void register();

    public native void register(LoginOptions options);

    public native boolean hasRealmRole(String role);

    public native boolean hasResourceRole(String role, String resource);

    public native boolean isTokenExpired();

    public native boolean isTokenExpired(int minValiditySeconds);

    public native KeycloakCallback updateToken();

    public native KeycloakCallback updateToken(int minValiditySeconds);

    public native void clearToken();

    public native void onTokenExpired(Runnable expiredFn);

    @JsProperty
    public Runnable onAuthSuccess;

    @JsProperty
    public Runnable onAuthLogout;
}
