package org.openremote.manager.client.interop.keycloak;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(isNative = true)
public interface AuthToken {

    @JsProperty
    String getName();

    @JsProperty(name = "preferred_username")
    String getPreferredUsername();

}
