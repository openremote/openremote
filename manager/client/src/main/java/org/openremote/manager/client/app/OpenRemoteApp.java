package org.openremote.manager.client.app;

import elemental2.dom.HTMLElement;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import org.openremote.manager.client.interop.keycloak.Keycloak;

/**
 * The singleton of {@code <or-app>}.
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "OpenRemoteApp")
public abstract class OpenRemoteApp extends HTMLElement {

    @JsProperty
    native public String getTenant();

    @JsProperty
    native public Keycloak getKeycloak();

    native public void logout();
}
