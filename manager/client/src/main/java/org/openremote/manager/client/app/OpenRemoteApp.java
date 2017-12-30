package org.openremote.manager.client.app;

import elemental.html.HtmlElement;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import org.openremote.manager.client.interop.keycloak.Keycloak;

/**
 * The singleton of {@code <or-app>}.
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "OpenRemoteApp")
public interface OpenRemoteApp {

    @JsProperty
    String getTenant();

    @JsProperty
    Keycloak getKeycloak();

    void logout();
}
